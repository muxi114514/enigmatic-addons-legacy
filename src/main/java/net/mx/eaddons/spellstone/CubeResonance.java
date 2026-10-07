package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneSwordConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 非欧共鸣「悖论之刃」：随机倍率伤害、按目标负面效果增伤、削半生命，
 * 外加折跃、效果对调与非欧领域（范围内的非玩家生物无法瞬移）。
 *
 * <p>伤害相关的几条在 {@link ResonanceEvents} 里结算（随机倍率与负面加成在护甲前，其中一部分按配置
 * 当作虚空伤害在护甲后加回；削半生命在护甲后），这里只放计算与主动。
 */
public final class CubeResonance {

    /** 对调的自建冷却键。 */
    public static final String SWAP_TIMER = "CubeSwap";

    private CubeResonance() {
    }

    // ============================ 伤害计算 ============================

    /**
     * 随机倍率：在 [下限, 上限] 之间均匀取值，每点幸运把下限抬高一档（有封顶）。
     * 幸运属性只有玩家注册，其它生物按 0 处理。
     */
    public static float rollMultiplier(EntityLivingBase attacker) {
        double min = Math.min(SpellstoneSwordConfig.cubeDamageMin
                + luckOf(attacker) * SpellstoneSwordConfig.cubeLuckFloorStep,
                SpellstoneSwordConfig.cubeLuckFloorCap);
        double max = SpellstoneSwordConfig.cubeDamageMax;
        if (min >= max) {
            return (float) max;
        }
        return (float) (min + attacker.getRNG().nextDouble() * (max - min));
    }

    private static double luckOf(EntityLivingBase entity) {
        IAttributeInstance luck = entity.getAttributeMap().getAttributeInstanceByName(
                SharedMonsterAttributes.LUCK.getName());
        return luck == null ? 0.0D : Math.max(0.0D, luck.getAttributeValue());
    }

    /** 目标每有一个负面效果就增伤一档，有封顶。 */
    public static float debuffBonus(EntityLivingBase target) {
        int count = 0;
        for (PotionEffect effect : target.getActivePotionEffects()) {
            Potion potion = effect.getPotion();
            if (potion != null && potion.isBadEffect()) {
                count++;
            }
        }
        return (float) Math.min(count * SpellstoneSwordConfig.cubeDebuffBonus,
                SpellstoneSwordConfig.cubeDebuffBonusCap);
    }

    /**
     * 悖论一击：按概率追加「目标当前生命的一半」。
     * Boss 按持剑者攻击力封顶，免得一次把 Boss 打掉半管血。
     *
     * @return 追加的伤害，未触发返回 0
     */
    public static float paradoxStrike(EntityLivingBase target, EntityLivingBase attacker) {
        if (attacker.getRNG().nextDouble() >= SpellstoneSwordConfig.cubeHalfChance) {
            return 0.0F;
        }
        float half = target.getHealth() * 0.5F;
        if (!target.isNonBoss()) {
            half = Math.min(half, ItemSpellstoneSword.getAttackDamage(attacker)
                    * (float) SpellstoneSwordConfig.cubeBossCapRatio);
        }
        if (half > 0.0F) {
            target.world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, target.getSoundCategory(), 0.8F, 0.6F);
        }
        return Math.max(0.0F, half);
    }

    // ============================ 虚空伤害：护甲前扣下、护甲后加回 ============================

    /** 每个目标当前这一击扣下的虚空份额。弱引用目标，加同步（整合服与客户端两个线程都可能触及）。 */
    private static final Map<EntityLivingBase, VoidPart> VOID_PARTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final class VoidPart {
        final DamageSource source;
        final float amount;

        VoidPart(DamageSource source, float amount) {
            this.source = source;
            this.amount = amount;
        }
    }

    public static void holdVoidPart(EntityLivingBase target, DamageSource source, float amount) {
        if (amount > 0.0F) {
            VOID_PARTS.put(target, new VoidPart(source, amount));
        } else {
            VOID_PARTS.remove(target);
        }
    }

    /** 取回同一伤害源实例扣下的份额；别的伤害源（上一击被取消留下的）作废。 */
    public static float takeVoidPart(EntityLivingBase target, DamageSource source) {
        VoidPart part = VOID_PARTS.remove(target);
        return part != null && part.source == source ? part.amount : 0.0F;
    }

    // ============================ 主动：折跃 ============================

    /** 沿视线瞬移，不穿墙：碰到方块就停在它前面，再由原版的落点校验找地面。 */
    public static boolean blink(World world, EntityPlayer player, ItemStack sword) {
        if (world.isRemote) {
            return true;
        }
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double distance = SpellstoneSwordConfig.cubeBlinkDistance;
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.add(look.scale(distance)), false, true, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            distance = Math.max(0.0D, eye.distanceTo(hit.hitVec) - 1.0D);
        }
        double fromX = player.posX;
        double fromY = player.posY;
        double fromZ = player.posZ;
        for (double d = distance; d >= 1.0D; d -= 2.0D) {
            Vec3d point = eye.add(look.scale(d));
            if (player.attemptTeleport(point.x, point.y, point.z)) {
                spawnPortal(world, fromX, fromY + player.height / 2.0D, fromZ);
                spawnPortal(world, player.posX, player.posY + player.height / 2.0D, player.posZ);
                player.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 0.8F, 1.4F);
                player.fallDistance = 0.0F;
                SwordCooldown.set(player, sword, SpellstoneSwordConfig.cubeBlinkCooldown);
                return true;
            }
        }
        return false;
    }

    // ============================ 主动：效果对调 ============================

    /**
     * 潜行右键：自己身上的负面效果与注视目标身上的正面效果互换。
     * <p>戴着非欧立方时玩家本就免疫负面效果，所以实战里多半等于「把目标的增益全偷过来」。
     */
    public static boolean swapEffects(World world, EntityPlayer player, ItemStack sword) {
        if (world.isRemote) {
            return true;
        }
        if (!SwordCooldown.ready(sword, SWAP_TIMER)) {
            return false;
        }
        List<EntityLivingBase> observed = SpellstoneAbilities.getObservedEntities(player, world, 1.2D, 16);
        if (observed.isEmpty()) {
            return false;
        }
        EntityLivingBase target = observed.get(0);
        List<PotionEffect> mine = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion() != null && effect.getPotion().isBadEffect()) {
                mine.add(effect);
            }
        }
        List<PotionEffect> theirs = new ArrayList<>();
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (effect.getPotion() != null && !effect.getPotion().isBadEffect()) {
                theirs.add(effect);
            }
        }
        if (mine.isEmpty() && theirs.isEmpty()) {
            return false;
        }
        for (PotionEffect effect : mine) {
            player.removePotionEffect(effect.getPotion());
            target.addPotionEffect(copyOf(effect));
        }
        for (PotionEffect effect : theirs) {
            target.removePotionEffect(effect.getPotion());
            player.addPotionEffect(copyOf(effect));
        }
        spawnPortal(world, target.posX, target.posY + target.height / 2.0D, target.posZ);
        player.playSound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 1.0F, 0.8F);
        player.sendStatusMessage(new TextComponentTranslation("message.eaddons.resonance.swap"), true);
        SwordCooldown.start(sword, SWAP_TIMER, SpellstoneSwordConfig.cubeSwapCooldown);
        return true;
    }

    private static PotionEffect copyOf(PotionEffect effect) {
        return new PotionEffect(effect.getPotion(), effect.getDuration(), effect.getAmplifier(),
                effect.getIsAmbient(), effect.doesShowParticles());
    }

    // ============================ 非欧领域：禁止瞬移 ============================

    /**
     * 该生物是否处在某个非欧共鸣者的领域内。玩家不受影响。
     * <p>只遍历本维度的玩家列表（通常个位数），比维护一份持剑者注册表更省事也更不易出错。
     */
    public static boolean domainBlocks(EntityLivingBase entity) {
        if (entity == null || entity instanceof EntityPlayer || entity.world == null || entity.world.isRemote) {
            return false;
        }
        double radiusSq = SpellstoneSwordConfig.cubeDomainRadius * SpellstoneSwordConfig.cubeDomainRadius;
        if (radiusSq <= 0.0D) {
            return false;
        }
        for (EntityPlayer player : entity.world.playerEntities) {
            ItemStack held = player.getHeldItemMainhand();
            if (!(held.getItem() instanceof ItemSpellstoneSword) || !SpellstoneData.isCubeActive(held)) {
                continue;
            }
            if (player.getDistanceSq(entity) <= radiusSq) {
                spawnPortal(entity.world, entity.posX, entity.posY + entity.height / 2.0D, entity.posZ);
                return true;
            }
        }
        return false;
    }

    private static void spawnPortal(World world, double x, double y, double z) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 12,
                    0.4D, 0.6D, 0.4D, 0.4D);
        }
    }
}
