package net.mx.eaddons.item;

import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 混沌鞘翅的服务端逻辑：飞行计时/动量跟踪、混沌俯冲 AOE、定向减伤。
 * 客户端输入/加速/粒子在 {@code ChaosElytraClientHandler}。
 */
public class ChaosElytraEventHandler {
    /** 深渊伤害源：绕甲魔法伤害，对应 1.20 的 ABYSS。 */
    public static final DamageSource ABYSS = new DamageSource("abyss").setDamageBypassesArmor().setMagicDamage();

    private final Map<EntityPlayer, Integer> flyingTicks = new WeakHashMap<>();
    private final Map<EntityPlayer, Vec3d> lastMovement = new WeakHashMap<>();

    /**
     * 鞘翅飞行位维护（服务端权威，END 阶段）。
     * <p>1.12.2 的鞘翅飞行由实体第 7 位标志驱动，而该标志是 DataManager 项、由服务端权威同步给客户端。
     * 若客户端也去写它（乐观置位/落地清除），客户端 onGround 抖动就会与服务端相互覆盖，第三人称表现为
     * 站立飞行与鞘翅飞行来回切换、很卡顿。故 flag7 完全交由服务端拥有：客户端只发"起飞请求"。
     * <p>放在 {@code END} 阶段——即 vanilla {@code travel()} 之后——重置，保证本 tick 结尾 flag7 与飞行意图一致，
     * 不会被 travel() 里的落地清除等逻辑在 tick 中途改掉后残留到发包。仅管理由本系统发起飞行（意图位为真）的
     * 玩家，绝不干扰原版鞘翅。落地/入水/脱下即清意图并退出。
     */
    @SubscribeEvent
    public void onServerPostTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        if (!player.getEntityData().getBoolean("ChaosElytraWantsFly")) return;

        boolean worn = !ChaosElytraHelper.getChaosElytra(player).isEmpty();
        if (!worn || player.onGround || player.isInWater()) {
            player.getEntityData().setBoolean("ChaosElytraWantsFly", false);
            if (player.isElytraFlying()) {
                ChaosElytraHelper.setElytraFlying(player, false);
            }
        } else {
            // 空中且佩戴：幂等重置飞行位（值不变不发包）
            ChaosElytraHelper.setElytraFlying(player, true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.world.isRemote) return;
        EntityPlayer player = event.player;

        // 飞行计时（离地且滑翔中）
        if (!player.onGround && player.isElytraFlying()) {
            flyingTicks.put(player, flyingTicks.getOrDefault(player, 0) + 1);
        } else {
            flyingTicks.put(player, 0);
        }

        ItemStack elytra = ChaosElytraHelper.getChaosElytra(player);
        if (elytra.isEmpty()) {
            lastMovement.remove(player);
            return;
        }

        // 每 3 tick 记录动量（供俯冲力度与范围）
        if (player.ticksExisted % 3 == 0) {
            lastMovement.put(player, player.isElytraFlying()
                    ? new Vec3d(player.motionX, player.motionY, player.motionZ) : Vec3d.ZERO);
        }

        // 加速时消耗耐久
        if (player.getEntityData().getBoolean("ChaosElytraBoosting") && player.isElytraFlying()) {
            if (player.ticksExisted % 6 == 0) {
                elytra.damageItem(1, player);
            }
        }
    }

    // ============================ 混沌俯冲 ============================

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;
        if (!SuperpositionHandler.isTheWorthyOne(player) || ChaosElytraHelper.getChaosElytra(player).isEmpty()) return;

        if (player.getEntityData().getBoolean("ChaosElytraBoosting")) {
            event.setCanceled(true); // 抵消坠落伤害
            chaosDescending(player);
        }
    }

    private void chaosDescending(EntityPlayer player) {
        World world = player.world;
        Vec3d look = player.getLookVec();
        int flyTick = flyingTicks.getOrDefault(player, 0);

        if (look.y >= -0.95 || player.getCooldownTracker().hasCooldown(ItemChaosElytra.INSTANCE)
                || !spaceCheck(player.getPosition(), world) || flyTick <= 36) {
            return;
        }

        if (!player.capabilities.isCreativeMode) {
            player.getCooldownTracker().setCooldown(ItemChaosElytra.INSTANCE, ChaosElytraConfig.descendingCooldown);
        }

        Vec3d motion = lastMovement.getOrDefault(player, Vec3d.ZERO);
        double range = 3.5 + motion.lengthVector();

        // 冲击波粒子：必须用 WorldServer 的广播重载，普通 spawnParticle 在服务端不发包
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, player.posX, player.posY, player.posZ,
                    (int) (8 + range), range * 0.5, 0.3, range * 0.5, 0.0);
            ws.spawnParticle(EnumParticleTypes.PORTAL, player.posX, player.posY + 0.5, player.posZ,
                    (int) (30 + range * 6), range * 0.5, 0.5, range * 0.5, 0.4);
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, player.posX, player.posY + 0.2, player.posZ,
                    (int) (20 + range * 4), range * 0.5, 0.2, range * 0.5, 0.05);
        }

        double attackDamage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                EntityLivingBase.class, player.getEntityBoundingBox().grow(range));
        for (EntityLivingBase entity : entities) {
            if (entity == player) continue;
            Vec3d delta = entity.getPositionVector().subtract(player.getPositionVector()).normalize().scale(0.5);
            float modifier = (float) Math.min(1.0, 1.2 / Math.max(0.1, entity.getDistance(player)));
            Vec3d horiz = new Vec3d(delta.x, 0, delta.z).normalize().scale(modifier);
            entity.addVelocity(horiz.x, entity.onGround ? 1.2F * modifier : 0.0, horiz.z);
            entity.velocityChanged = true;

            double pow = Math.pow(ChaosElytraConfig.descendingPowerModifier, Math.abs(motion.y));
            entity.attackEntityFrom(ABYSS, (float) (attackDamage * pow));
        }
    }

    /** 判断脚下附近是否有足够空间可发动俯冲。带区块守卫。 */
    private static boolean spaceCheck(BlockPos pos, World world) {
        int space = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            if (!world.isBlockLoaded(p)) continue;
            if (world.getBlockState(p).getBlock().isAir(world.getBlockState(p), world, p)) {
                space += 3;
            } else {
                space -= 1;
            }
        }
        return space > 0;
    }

    // ============================ 定向减伤 ============================

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!SuperpositionHandler.isTheWorthyOne(player) || ChaosElytraHelper.getChaosElytra(player).isEmpty()) return;

        float modifier = ChaosElytraConfig.getDamageResistance();
        DamageSource source = event.getSource();

        boolean fallLike = source == DamageSource.FALL || source == DamageSource.FLY_INTO_WALL;
        if (fallLike) {
            applyResistance(event, modifier);
            return;
        }

        // 背后来袭（鞘翅护住后背）才减伤：攻击者相对玩家在朝向的反方向
        Entity direct = source.getImmediateSource();
        if (direct != null) {
            Vec3d toAttacker = direct.getPositionVector().subtract(player.getPositionVector());
            if (toAttacker.dotProduct(player.getLookVec()) < 0) {
                applyResistance(event, modifier);
            }
        }
    }

    private static void applyResistance(LivingHurtEvent event, float modifier) {
        if (modifier >= 1.0F) {
            event.setCanceled(true);
        } else {
            event.setAmount(event.getAmount() * (1.0F - modifier));
        }
    }
}
