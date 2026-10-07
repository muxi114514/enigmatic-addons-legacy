package net.mx.eaddons.util;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;

/**
 * 在 LivingHurtEvent（护甲结算前）估算一次伤害最终会扣掉多少生命。
 *
 * <p>顺序照原版 {@code EntityLivingBase#damageEntity}：护甲 → 抗性药水 → 保护类附魔 → 伤害吸收，
 * 只计算、不磨损护甲。装了 First Aid 时它按身体部位分别套用该部位的护甲，这里用的是全身总护甲，只是近似。
 */
public final class DamageEstimate {

    private DamageEstimate() {
    }

    public static float taken(EntityLivingBase victim, DamageSource source, float amount) {
        float damage = amount;
        if (!source.isUnblockable()) {
            IAttributeInstance toughness = victim.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) victim.getTotalArmorValue(),
                    toughness == null ? 0.0F : (float) toughness.getAttributeValue());
        }
        if (!source.isDamageAbsolute()) {
            PotionEffect resistance = victim.getActivePotionEffect(MobEffects.RESISTANCE);
            if (resistance != null && source != DamageSource.OUT_OF_WORLD) {
                damage = damage * (25 - (resistance.getAmplifier() + 1) * 5) / 25.0F;
            }
            if (damage > 0.0F) {
                int protection = EnchantmentHelper.getEnchantmentModifierDamage(victim.getArmorInventoryList(), source);
                if (protection > 0) {
                    damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float) protection);
                }
            }
        }
        return Math.max(damage - victim.getAbsorptionAmount(), 0.0F);
    }
}
