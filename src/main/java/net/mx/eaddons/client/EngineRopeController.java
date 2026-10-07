package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.entity.EntityEngineHook;
import net.mx.eaddons.entity.HookRope;
import net.mx.eaddons.spellstone.EngineHookDetachMessage;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * 失落引擎抓钩勾在方块上时，持有者本人的绳索物理（照整合包 grapplemod 的 {@code grappleController}）。
 *
 * <p>放在客户端：玩家移动由客户端决定，服务端改速度要靠速度包覆盖客户端，延迟一高就顿挫、回弹。
 * 自己维护速度：原版空中水平速度每 tick ×0.91，靠原版物理荡不起来；这里只留 0.5% 空气阻力，每 tick 覆盖玩家速度。
 *
 * <p>流程：先自动收绳把人拉到锚点附近，到位或卡住后转为挂绳。挂绳时绳长固定，人越出绳长就去掉背离锚点的速度并拉回，
 * 于是像钟摆一样吊在锚点下方。键位同 grapplemod：WASD 摆荡、按住潜行减速、潜行+前/后收放绳、跳跃沿绳跃出并脱钩。
 */
@SideOnly(Side.CLIENT)
public final class EngineRopeController {

    // 数值对齐整合包 grappling_hook.cfg：攀爬 0.3、摆荡系数 0.5、跳离力度 1.0、断绳缓冲 5
    private static final double GRAVITY = 0.05D;
    private static final double AIR_DRAG = 0.005D;
    private static final double LIQUID_DRAG = 0.25D;
    private static final double SWING_FACTOR = 0.5D;
    private static final double BRAKE = 0.85D;
    private static final double CLIMB_SPEED = 0.3D;
    private static final double JUMP_POWER = 1.0D;
    private static final double SNAP_BUFFER = 5.0D;
    /** 收绳加速度与沿绳最高速度（格/tick）。 */
    private static final double REEL_ACCEL = 0.2D;
    private static final double REEL_MAX_SPEED = 1.6D;
    /**
     * 快到位时沿绳限速 = 剩余距离 × 该系数 + 0.15，平稳刹停。
     * 不限的话 1.6 格/tick 一步就冲过挂绳距离，剩下的速度对新绳向成了切向，人会绕锚点甩半圈撞墙。
     */
    private static final double REEL_ARRIVE = 0.4D;
    /** 收绳时横向速度每 tick 保留的比例，抑制绕着锚点打转。 */
    private static final double REEL_SIDE_DAMP = 0.85D;
    /** 贴地收绳时先给一点起跳，免得被拖着在地上走。 */
    private static final double REEL_LIFT = 0.25D;
    /** 收到眼睛离锚点这么近就转为挂绳。 */
    private static final double HANG_LENGTH = 2.0D;
    /** 收绳这么多 tick 没再拉近就视为卡住，就地挂绳。 */
    private static final int REEL_STALL_TICKS = 20;
    /** 挂绳时眼睛离锚点的最短距离，再近头就碰到钩子（会被当成捡回收钩）。 */
    private static final double MIN_REMAINING = 1.0D;
    /** 最长绳长，给服务端「离钩 64 格收钩」留余量。 */
    private static final double MAX_LENGTH = 60.0D;
    /** 离地后几 tick 内仍按地面处理，同 grapplemod。 */
    private static final int GROUND_GRACE = 3;

    /** 本地玩家同一时间只有一只钩；弱引用钩子，钩子消失后不拖住实体。 */
    @Nullable private static EngineRopeController active;

    private final WeakReference<EntityEngineHook> hookRef;
    private final HookRope rope = new HookRope();
    private Vec3d motion;
    /** 钩子沿绳到玩家眼睛的总绳长。 */
    private double length;
    private double bestLength;
    private boolean reeling = true;
    private boolean released;
    private int stallTicks;
    private int groundTimer;
    /** 初值为真：勾中时若正按着跳跃，要先松开再按才算跃出。 */
    private boolean prevJump = true;

    private EngineRopeController(EntityEngineHook hook, EntityPlayerSP player) {
        this.hookRef = new WeakReference<>(hook);
        // 取本 tick 的实际位移作初速度，比读 motion（已被原版阻力削过）更接近真实
        this.motion = new Vec3d(player.posX - player.prevPosX, player.posY - player.prevPosY,
                player.posZ - player.prevPosZ);
        Vec3d hookPos = hook.getPositionVector();
        Vec3d eye = eyeOf(player);
        this.rope.update(player.world, hookPos, eye);
        this.length = this.rope.lengthToAnchor(hookPos) + this.rope.anchorFor(hookPos).distanceTo(eye);
        this.bestLength = this.length;
    }

    /** 由客户端钩子每 tick 调用；只接管本地玩家自己的钩。 */
    public static void tick(EntityEngineHook hook) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || hook.getOwner() != player) {
            return;
        }
        EngineRopeController controller = active;
        if (controller == null || controller.hookRef.get() != hook) {
            controller = new EngineRopeController(hook, player);
            active = controller;
        }
        controller.update(hook, player);
    }

    /** 本人的链条按本地绳段画（与物理一致），其他玩家的按服务端同步的画。 */
    public static HookRope ropeFor(EntityEngineHook hook) {
        EngineRopeController controller = active;
        return controller != null && controller.hookRef.get() == hook ? controller.rope : hook.getRope();
    }

    private void update(EntityEngineHook hook, EntityPlayerSP player) {
        if (this.released) {
            return;   // 已跃出，等服务端收钩
        }
        if (player.isRiding() || player.capabilities.isFlying || player.isElytraFlying()) {
            release();   // 骑乘、创造飞行、鞘翅滑翔时不再由绳子接管
            return;
        }
        MovementInput input = player.movementInput;
        boolean jumpPressed = input.jump && !this.prevJump;
        this.prevJump = input.jump;

        if (player.onGround) {
            this.groundTimer = GROUND_GRACE;
        } else if (this.groundTimer > 0) {
            this.groundTimer--;
        }
        boolean grounded = this.groundTimer > 0;
        if (grounded) {
            // 着地交给原版走路，绳子只限制活动范围
            this.motion = new Vec3d(player.motionX, player.motionY, player.motionZ);
        } else {
            applyCollisions(player);
            double drag = player.isInWater() || player.isInLava() ? LIQUID_DRAG : AIR_DRAG;
            this.motion = new Vec3d(this.motion.x, this.motion.y - GRAVITY, this.motion.z).scale(1.0D - drag);
        }

        Vec3d hookPos = hook.getPositionVector();
        Vec3d eye = eyeOf(player);
        this.rope.update(player.world, hookPos, eye);
        Vec3d anchor = this.rope.anchorFor(hookPos);
        double toAnchor = this.rope.lengthToAnchor(hookPos);
        Vec3d radial = eye.subtract(anchor);
        double dist = radial.lengthVector();
        if (dist < 1.0E-4D) {
            return;
        }
        Vec3d outward = radial.scale(1.0D / dist);   // 锚点 → 玩家

        if (jumpPressed && !grounded) {
            jumpOff(player, outward, dist);
            return;
        }

        Vec3d extra = Vec3d.ZERO;
        if (this.reeling) {
            updateReel(outward, toAnchor + dist, toAnchor, player.onGround);
        } else if (input.sneak) {
            // 按住潜行：刹住摆荡；同时按前 / 后收放绳
            this.motion = new Vec3d(this.motion.x * BRAKE, this.motion.y, this.motion.z * BRAKE);
            int climb = input.forwardKeyDown == input.backKeyDown ? 0 : input.forwardKeyDown ? 1 : -1;
            if (climb != 0) {
                this.length = toAnchor + dist - climb * CLIMB_SPEED;
                if (climb < 0 && outward.y < 0.0D) {
                    extra = new Vec3d(0.0D, outward.y * CLIMB_SPEED, 0.0D);   // 放绳时立刻往下送，不等重力
                }
            }
        } else if (!grounded) {
            swing(player, input);
        }
        this.length = MathHelper.clamp(this.length, toAnchor + MIN_REMAINING, MAX_LENGTH);

        double remaining = this.length - toAnchor;
        if (dist > remaining) {
            if (dist - remaining > SNAP_BUFFER) {
                release();   // 被传送或击飞到远超绳长，视为断绳
                return;
            }
            extra = extra.add(outward.scale(remaining - dist));   // 拉回绳长以内（只作用这一 tick 的位移）
        }
        // 下一 tick 会越出绳长 → 去掉背离锚点的速度分量，只剩切向，于是绕锚点摆荡
        double away = this.motion.dotProduct(outward);
        if (away > 0.0D && eye.add(this.motion).distanceTo(anchor) > remaining) {
            this.motion = this.motion.subtract(outward.scale(away));
        }
        applyMotion(player, extra);
    }

    /** 收绳：绳长跟着人收短（拉近后不会再掉回去），到位或卡住转为挂绳。 */
    private void updateReel(Vec3d outward, double pathLength, double toAnchor, boolean onGround) {
        this.length = Math.min(this.length, pathLength);
        if (this.length < this.bestLength - 0.05D) {
            this.bestLength = this.length;
            this.stallTicks = 0;
        }
        double left = this.length - toAnchor - HANG_LENGTH;
        if (left <= 0.05D || ++this.stallTicks > REEL_STALL_TICKS) {
            // 到位刹停：去掉朝锚点的速度，否则会冲过头顶到钩子
            this.reeling = false;
            double toward = this.motion.dotProduct(outward);
            if (toward < 0.0D) {
                this.motion = this.motion.subtract(outward.scale(toward));
            }
            return;
        }
        Vec3d inward = outward.scale(-1.0D);
        double along = this.motion.dotProduct(inward);
        Vec3d side = this.motion.subtract(inward.scale(along)).scale(REEL_SIDE_DAMP);
        double cap = Math.min(REEL_MAX_SPEED, left * REEL_ARRIVE + 0.15D);
        along = Math.min(cap, along + REEL_ACCEL);
        this.motion = side.add(inward.scale(along));
        if (onGround && inward.y > -0.5D && this.motion.y < REEL_LIFT) {
            this.motion = new Vec3d(this.motion.x, REEL_LIFT, this.motion.z);
        }
    }

    /** WASD 摆荡：朝输入方向加速，越快加得越多（同 grapplemod applyPlayerMovement）。 */
    private void swing(EntityPlayerSP player, MovementInput input) {
        float strafe = input.moveStrafe;
        float forward = input.moveForward;
        if (strafe * strafe + forward * forward < 1.0E-4F) {
            return;
        }
        // 同原版 moveRelative 的朝向换算
        float sin = MathHelper.sin(player.rotationYaw * 0.017453292F);
        float cos = MathHelper.cos(player.rotationYaw * 0.017453292F);
        Vec3d dir = new Vec3d(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin).normalize();
        double accel = (0.015D + this.motion.lengthVector() * 0.01D) * SWING_FACTOR;
        this.motion = this.motion.add(dir.scale(accel));
    }

    /** 跳跃跃出：沿绳朝锚点弹出（同 grapplemod rope_jump_at_angle），已有的朝锚点速度抵扣力度；人在锚点上方时不弹。 */
    private void jumpOff(EntityPlayerSP player, Vec3d outward, double dist) {
        if (outward.y <= 0.0D && dist > 1.0D) {
            Vec3d inward = outward.scale(-1.0D);
            double power = JUMP_POWER - Math.max(0.0D, this.motion.dotProduct(inward));
            if (power > 0.0D) {
                this.motion = this.motion.add(inward.scale(power));
            }
        }
        applyMotion(player, Vec3d.ZERO);
        release();
    }

    /** 撞墙、顶到天花板时清掉对应分量，否则自维护的速度会一直往墙里推（同 grapplemod normalCollisions）。 */
    private void applyCollisions(EntityPlayerSP player) {
        double x = this.motion.x;
        double y = this.motion.y;
        double z = this.motion.z;
        if (player.collidedHorizontally) {
            // 原版 move 撞到哪个轴就把哪个轴的 motion 置 0
            if (player.motionX == 0.0D) {
                x = 0.0D;
            }
            if (player.motionZ == 0.0D) {
                z = 0.0D;
            }
        }
        if (player.collidedVertically && y > 0.0D && player.posY == player.lastTickPosY) {
            y = 0.0D;
        }
        this.motion = new Vec3d(x, y, z);
    }

    private void applyMotion(EntityPlayerSP player, Vec3d extra) {
        Vec3d out = this.motion.add(extra);
        if (Double.isNaN(out.x) || Double.isNaN(out.y) || Double.isNaN(out.z)) {
            this.motion = Vec3d.ZERO;
            out = Vec3d.ZERO;
        }
        player.motionX = out.x;
        player.motionY = out.y;
        player.motionZ = out.z;
        player.fallDistance = 0.0F;
    }

    /** 交还原版物理并通知服务端收钩；速度保留，松手后按惯性飞出。 */
    private void release() {
        this.released = true;
        EAddonsMod.PACKET_HANDLER.sendToServer(new EngineHookDetachMessage());
    }

    private static Vec3d eyeOf(EntityPlayerSP player) {
        return new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    }
}
