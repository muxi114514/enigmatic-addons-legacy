package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.ChaosElytraConfig;
import net.mx.eaddons.item.ChaosElytraHelper;
import net.mx.eaddons.item.ChaosElytraStateMessage;

/**
 * 混沌鞘翅客户端处理：检测跳跃输入以开启鞘翅飞行、加速冲刺、生成拖尾粒子。
 * 翅膀层的注册在 ClientProxy（skinMap），仅在客户端注册，负责本地玩家。
 */
@SideOnly(Side.CLIENT)
public class ChaosElytraClientHandler {
    private boolean boosting;
    private boolean wasJumpDown;
    private boolean wantFly; // 起飞请求的本地锁存：持续请求直到服务端授予或落地

    @SubscribeEvent
    public void onClientTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || player != event.player || !player.world.isRemote) return;

        boolean jump = mc.gameSettings.keyBindJump.isKeyDown();
        boolean jumpPressed = jump && !wasJumpDown; // 仅取"本 tick 新按下"的上升沿
        wasJumpDown = jump;

        if (ChaosElytraHelper.getChaosElytra(player).isEmpty()) {
            wantFly = false;
            stopBoost();
            return;
        }

        // flag7 由服务端权威拥有，客户端只发"起飞请求"、绝不本地写标志——
        // 否则客户端 onGround 抖动会与服务端维持相互覆盖，第三人称就会站立/鞘翅来回切换。
        // 空中"再次按下"跳跃开始请求（避免地面起跳被误判为起飞，对齐原版双击空格语义）
        if (jumpPressed && !player.onGround && !player.isElytraFlying() && !player.isInWater()) {
            wantFly = true;
        }
        // 持续请求直到服务端授予（isElytraFlying 同步为真）或落地/入水——
        // 克服"起飞请求包早于移动包到达、服务端仍判定 onGround 而一次性拒绝"的时序问题
        if (wantFly) {
            if (player.isElytraFlying() || player.onGround || player.isInWater()) {
                wantFly = false;
            } else {
                EAddonsMod.PACKET_HANDLER.sendToServer(new ChaosElytraStateMessage(ChaosElytraStateMessage.START_FLY));
            }
        }

        if (player.isElytraFlying() && jump) {
            boostPlayer(player);
            if (!boosting) {
                boosting = true;
                EAddonsMod.PACKET_HANDLER.sendToServer(new ChaosElytraStateMessage(ChaosElytraStateMessage.START_BOOST));
            }
            spawnTrail(player);
        } else {
            stopBoost();
        }
    }

    private void stopBoost() {
        if (boosting) {
            boosting = false;
            EAddonsMod.PACKET_HANDLER.sendToServer(new ChaosElytraStateMessage(ChaosElytraStateMessage.STOP_BOOST));
        }
    }

    private static void boostPlayer(EntityPlayer player) {
        Vec3d look = player.getLookVec().scale(ChaosElytraConfig.flyingSpeedModifier);
        double mx = player.motionX, my = player.motionY, mz = player.motionZ;
        player.motionX += look.x * 0.1 + (look.x * 1.5 - mx) * 0.5;
        player.motionY += look.y * 0.1 + (look.y * 1.5 - my) * 0.5;
        player.motionZ += look.z * 0.1 + (look.z * 1.5 - mz) * 0.5;
    }

    private static void spawnTrail(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < 3; i++) {
            double x = player.posX + (Math.random() - 0.5);
            double y = player.posY + player.getEyeHeight() - 0.5 + (Math.random() - 0.5);
            double z = player.posZ + (Math.random() - 0.5);
            mc.effectRenderer.addEffect(new ParticleAbyssChaos(player.world, x, y, z,
                    (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2));
        }
    }
}
