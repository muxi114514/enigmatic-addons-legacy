package net.mx.eaddons.spellstone;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * 原地横扫。
 *
 * <p>1.13 才有公开的 {@code Player#sweepAttack()}，1.12.2 的横扫写死在
 * {@code EntityPlayer#attackTargetEntityWithCurrentItem} 内部、没有入口，
 * 故按原版那段逻辑（范围、击退方向、音效、粒子）重写一份，供烈焰松手与星云瞬移斩调用。
 */
public final class SweepHelper {

    private SweepHelper() {
    }

    /**
     * 以玩家为中心横扫一圈。
     *
     * @param damage 每个目标承受的伤害；传 0 表示按原版规则取横扫之刃加成后的基础值
     * @param radius 横扫半径，原版等效值约 3.0
     */
    public static void sweep(EntityPlayer player, float damage, double radius) {
        float amount = damage > 0.0F ? damage : 1.0F + EnchantmentHelper.getSweepingDamageRatio(player);
        double radiusSq = radius * radius;
        float rad = player.rotationYaw * 0.017453292F;
        double kbX = MathHelper.sin(rad);
        double kbZ = -MathHelper.cos(rad);

        List<EntityLivingBase> list = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(radius, 0.25D, radius));
        for (EntityLivingBase target : list) {
            if (!AoeTargets.canHit(player, target) || player.getDistanceSq(target) > radiusSq) {
                continue;
            }
            target.knockBack(player, 0.4F, kbX, kbZ);
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), amount);
        }

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(), 1.0F, 1.0F);
        player.spawnSweepParticles();
    }
}
