package net.mx.eaddons.spellstone;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneFormConfig;
import net.mx.eaddons.util.DamageEstimate;

/**
 * 持剑者受击时的防御：魔像 / 海洋的持握减伤、忘却冰盾、虚空弹反。
 * 事件订阅留在 {@link SpellstoneSwordEvents}，这里只放结算。
 */
final class SpellstoneHolderDefense {

    private SpellstoneHolderDefense() {
    }

    // ============================ 持握减伤与冰盾 ============================

    /**
     * 魔像近战与弹射物按配置减伤、海洋湿润 ×0.6、忘却冰盾按能量抵伤。
     *
     * @param preArmor amount 是护甲结算前的数值时，冰盾按估算的护甲后伤害扣能量，没抵完的部分按比例折回护甲前
     */
    static float holderReduction(EntityLivingBase entity, DamageSource source, float amount, boolean preArmor) {
        ItemStack held = entity.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemSpellstoneSword)) {
            return amount;
        }
        boolean physical = SpellstoneSwordEvents.isMelee(source) || source.isProjectile();
        // 原初共鸣：魔像与海洋这两条持握被动同时常驻，所以叠乘而不是二选一
        if (SpellstoneData.isPrimevalActive(held)) {
            if (physical) {
                amount *= (float) SpellstoneFormConfig.golemDamageTaken;
            }
            if (entity.isWet()) {
                amount *= 0.6F;
            }
            return SpellstoneData.getSubForm(held) == SpellstoneForm.FORGOTTEN_ICE
                    ? iceShield(entity, source, amount, preArmor, held) : amount;
        }
        if (SpellstoneData.isResonatingWith(held, SpellstoneForm.GOLEM_HEART) && physical) {
            return amount * (float) SpellstoneFormConfig.golemDamageTaken;
        }
        if (SpellstoneData.isResonatingWith(held, SpellstoneForm.OCEAN_STONE) && entity.isWet()) {
            return amount * 0.6F;
        }
        if (SpellstoneData.isResonatingWith(held, SpellstoneForm.FORGOTTEN_ICE)) {
            return iceShield(entity, source, amount, preArmor, held);
        }
        return amount;
    }

    /** 忘却的冰盾：按能量抵伤，抵不完的部分按比例折回。 */
    private static float iceShield(EntityLivingBase entity, DamageSource source, float amount,
                                   boolean preArmor, ItemStack held) {
        int shield = SpellstoneData.getEnergy(held);
        if (shield <= 0) {
            return amount;
        }
        float taken = preArmor ? DamageEstimate.taken(entity, source, amount) : amount;
        if (taken <= shield) {
            SpellstoneData.setEnergy(held, shield - (int) taken);
            return 0.0F;
        }
        SpellstoneData.setEnergy(held, 0);
        return amount * (taken - shield) / taken;
    }

    // ============================ 虚空弹反 ============================

    /** 正在虚空蓄力格挡（剑盾的普通举盾不算），且这一击可以弹反：正面、非穿甲。 */
    static boolean canParry(EntityLivingBase target, DamageSource source) {
        Entity direct = source.getImmediateSource();
        if (!target.isHandActive() || direct == null || source.isUnblockable()) {
            return false;
        }
        ItemStack active = target.getActiveItemStack();
        return active.getItem() instanceof ItemSpellstoneSword
                && SpellstoneData.isResonatingWith(active, SpellstoneForm.VOID_PEARL)
                && !SwordShieldUse.isShieldUse(active)
                && isFrontal(target, direct);
    }

    /** 原初共鸣下虚空的格挡（举起与弹反）不进冷却。 */
    static boolean parryCooldownWaived(ItemStack sword) {
        return SpellstoneFormConfig.primevalVoidParryNoCooldown && SpellstoneData.isPrimevalActive(sword);
    }

    /**
     * 弹反：取消这一击，周围敌人吃攻击力倍率的伤害并被击退，攻击者另吃被挡伤害的一部分（魔法伤害），按共鸣等级充能。
     *
     * @param damage 被挡下的原始伤害（攻击事件里的数值，还没被原版格挡清零）
     */
    static void parry(EntityLivingBase blocker, Entity direct, float damage) {
        ItemStack useItem = blocker.getActiveItemStack();
        useItem.damageItem(Math.max(1, (int) (damage / 2.0F)), blocker);
        boolean waived = parryCooldownWaived(useItem);
        if (blocker instanceof EntityPlayer) {
            ((EntityPlayer) blocker).getCooldownTracker().removeCooldown(useItem.getItem());
        }
        blocker.resetActiveHand();
        SwordShieldUse.endUse(useItem);
        blocker.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 2, 0, true, false));
        blocker.swingArm(blocker.getActiveHand());

        if (direct instanceof EntityLivingBase && direct != blocker) {
            float area = ItemSpellstoneSword.getAttackDamage(blocker) * (float) SpellstoneFormConfig.voidParryRatio;
            AxisAlignedBB box = blocker.getEntityBoundingBox().grow(3.2D);
            for (EntityLivingBase e : blocker.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                if (!AoeTargets.canHit(blocker, e)) {
                    continue;
                }
                e.knockBack(blocker, 0.5F, blocker.posX - e.posX, blocker.posZ - e.posZ);
                e.attackEntityFrom(DamageSource.causeMobDamage(blocker), area);
                e.hurtResistantTime = 5;
            }
            float reflect = damage * (float) SpellstoneFormConfig.voidParryReflect;
            if (reflect > 0.0F && direct.isEntityAlive()) {
                direct.hurtResistantTime = 0;
                direct.attackEntityFrom(SpellstoneDamage.magic(blocker), reflect);
            }
        }
        if (blocker instanceof EntityPlayer && !waived) {
            SwordCooldown.set((EntityPlayer) blocker, useItem, 21);
        }
        SpellstoneData.setEnergy(useItem, SpellstoneData.getLevel(useItem) > 4 ? 4 : 2);
    }

    /** 攻击者是否在格挡者正面。 */
    private static boolean isFrontal(EntityLivingBase blocker, Entity attacker) {
        double dx = attacker.posX - blocker.posX;
        double dz = attacker.posZ - blocker.posZ;
        Vec3d look = blocker.getLookVec();
        return dx * look.x + dz * look.z > 0.0D;
    }
}
