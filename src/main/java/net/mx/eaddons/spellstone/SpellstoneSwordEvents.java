package net.mx.eaddons.spellstone;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.mx.eaddons.compat.ModCompat;
import net.mx.eaddons.item.EntitySoulFlameBall;
import net.mx.eaddons.item.FrostHelper;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneFormConfig;
import net.mx.eaddons.item.SpellstoneSwordConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 术质共鸣者的被动：命中增伤、受伤减免、弹反、击杀与暴击效果。
 * 对应 1.20.1 的 {@code SpellstoneSwordEvents}，事件名在 1.12.2 一一对应。
 */
public class SpellstoneSwordEvents {

    // ============================ 护甲结算前 ============================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        EntityLivingBase target = event.getEntityLiving();
        DamageSource source = event.getSource();

        // 攻击者持剑：魔像按护甲增伤 / 引擎满能量全力一击
        Entity direct = source.getImmediateSource();
        if (direct instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) direct;
            ItemStack weapon = attacker.getHeldItemMainhand();
            if (isSword(weapon)) {
                int level = SpellstoneData.getLevel(weapon);
                if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.GOLEM_HEART)) {
                    event.setAmount(event.getAmount() + target.getTotalArmorValue() * (float) SpellstoneFormConfig.golemArmorBonus);
                    if (level > 4) {
                        double kbr = Math.max(0.0D, target.getEntityAttribute(
                                net.minecraft.entity.SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue());
                        event.setAmount(event.getAmount() * (1.0F + (float) Math.pow(kbr, 0.9D) * 0.8F));
                    }
                } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.LOST_ENGINE) && level > 4
                        && SpellstoneData.getEnergy(weapon) >= SpellstoneData.getMaxEnergy(weapon)
                        && SpellstoneData.getMaxEnergy(weapon) > 0) {
                    if (!SpellstoneData.isCreative(attacker)) {
                        SpellstoneData.setEnergy(weapon, 0);
                    }
                    event.setAmount(event.getAmount() * (float) SpellstoneFormConfig.engineFullStrike);
                }
            }
        }

        ItemStack held = target.getHeldItemMainhand();
        if (!isSword(held)) {
            return;
        }
        // 炽焰满级：免疫一切火焰伤害。原初共鸣常驻十形态的持握被动，这条也在内
        if (source.isFireDamage() && (SpellstoneData.isPrimevalActive(held)
                || (SpellstoneData.isResonatingWith(held, SpellstoneForm.BLAZING_CORE)
                        && SpellstoneData.getLevel(held) > 4))) {
            event.setCanceled(true);
            return;
        }
    }

    /**
     * 虚空蓄力格挡中被正面、非穿甲攻击命中 → 弹反。放在攻击事件：原版格挡在受伤事件之前就把伤害清零了，
     * 这里才拿得到被挡下的原始伤害；取消攻击也免掉了格挡者自己挨的那下击退。
     */
    @SubscribeEvent
    public void onParryAttack(LivingAttackEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (!target.world.isRemote && SpellstoneHolderDefense.canParry(target, event.getSource())) {
            SpellstoneHolderDefense.parry(target, event.getSource().getImmediateSource(), event.getAmount());
            event.setCanceled(true);
        }
    }

    // ============================ 护甲结算后 ============================

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        DamageSource source = event.getSource();

        // 被伤者持剑：受伤减免
        event.setAmount(SpellstoneHolderDefense.holderReduction(entity, source, event.getAmount(), false));

        // 攻击者持剑：命中增伤
        Entity trueSource = source.getTrueSource();
        if (!(trueSource instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase attacker = (EntityLivingBase) trueSource;
        ItemStack weapon = attacker.getHeldItemMainhand();
        if (!isSword(weapon)) {
            return;
        }
        int level = SpellstoneData.getLevel(weapon);
        boolean playerAttack = isMelee(source);

        if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.BLAZING_CORE)) {
            // 炽热 1 级起就积攒，满级前增伤按系数打折
            float heat = HeatHelper.getDamageBonus(attacker);
            event.setAmount(event.getAmount() * (1.0F + (level > 4 ? heat
                    : heat * (float) SpellstoneFormConfig.blazingPreMaxHeatFactor)));
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.FORGOTTEN_ICE) && playerAttack) {
            // 冰盾按这一击造成的伤害积攒
            SpellstoneData.addEnergy(weapon, Math.max(SpellstoneFormConfig.frostShieldMin,
                    Math.round(event.getAmount() * (float) SpellstoneFormConfig.frostShieldRatio)));
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.OCEAN_STONE) && attacker.isWet()) {
            event.setAmount(event.getAmount() * 1.25F);
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.VOID_PEARL) && playerAttack) {
            voidFollowUp(entity, attacker, weapon, event.getAmount());
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.ANGEL_BLESSING)) {
            if (!attacker.onGround && attacker.world.isAirBlock(attacker.getPosition())) {
                event.setAmount(event.getAmount() * 1.6F);
            }
            if (level > 4) {
                int bless = entity.getEntityData().getInteger("ResonanceAngelBless");
                int count = (bless & 1) + ((bless & 2) >> 1) + ((bless & 4) >> 2);
                float modifier = count >= 3 ? 1.0F : count == 2 ? 0.5F : count == 1 ? 0.25F : 0.0F;
                event.setAmount(event.getAmount() * (1.0F + modifier));
                if (playerAttack) {
                    SpellstoneAbilities.markBless(entity, 1);
                }
            }
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.REVIVAL_LEAF) && playerAttack) {
            revivalOnHit(entity, attacker, weapon, event.getAmount());
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.ILLUSION_LANTERN) && playerAttack) {
            illusionOnHit(entity, attacker, weapon, event);
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.LOST_ENGINE)) {
            // 玩家的服务端 motion 不随客户端移动更新，按实际位移测速
            double speed = PlayerSpeedTracker.horizontalSpeed(attacker);
            int len = (int) Math.pow(attacker.fallDistance * 2.0D + speed * 10.0D, 0.9D);
            event.setAmount(event.getAmount() * (1.0F + (float) SpellstoneFormConfig.engineMomentumStep * len));
        }
        event.setAmount(primevalHoldingBonus(attacker, weapon, event.getAmount()));
    }

    /**
     * 原初共鸣常驻的十形态持握被动里的两条增伤（海洋湿润 +25%、天使空中 +60%）。
     * 当前子形态自己那条已经在上面的链里结算过，这里要跳过，免得乘两次。
     */
    private static float primevalHoldingBonus(EntityLivingBase attacker, ItemStack weapon, float amount) {
        if (!SpellstoneData.isPrimevalActive(weapon)) {
            return amount;
        }
        SpellstoneForm sub = SpellstoneData.getSubForm(weapon);
        if (sub != SpellstoneForm.OCEAN_STONE && attacker.isWet()) {
            amount *= 1.25F;
        }
        if (sub != SpellstoneForm.ANGEL_BLESSING && !attacker.onGround
                && attacker.world.isAirBlock(attacker.getPosition())) {
            amount *= 1.6F;
        }
        return amount;
    }

    /**
     * 装了 First Aid 时持剑受伤减免在护甲结算前再做一次；First Aid 按部位结算时也会发 LivingDamageEvent，
     * 这类玩家实际减免两次，当作特性保留（2026-10-04 用户确认）。LOW 排在 First Aid 的 LOWEST 之前。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHolderHurt(LivingHurtEvent event) {
        if (event.getAmount() >= Float.MAX_VALUE || !ModCompat.firstAidTakesOver(event.getEntityLiving())) {
            return;
        }
        event.setAmount(SpellstoneHolderDefense.holderReduction(event.getEntityLiving(), event.getSource(),
                event.getAmount(), true));
    }

    // ============================ 失落引擎：动能免疫 ============================

    /**
     * 抓钩把人高速甩出去，松钩后的下坠与撞墙都会结算伤害，玩这个形态等于自带自杀风险。
     * 参考奇异饰品「零惯性之石」的 {@code AbilityNullKinetic}：它在受击时比对
     * {@code DamageSource.FALL / FALLING_BLOCK / FLY_INTO_WALL} 三种来源直接判掉。
     * 这里只取与抓钩相关的两种——摔落与撞墙动能，被落石砸中仍照常受伤。
     *
     * <p>两个事件都接是为了双保险：{@link LivingFallEvent} 在伤害产生之前就掐掉，
     * {@link LivingAttackEvent} 兜住直接 {@code attackEntityFrom(FALL)} 的调用方，并负责撞墙那一路。
     * 攻击事件本就早于 First Aid 接管玩家伤害的 LivingHurtEvent，HIGHEST 只为抢在其它模组之前。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingFall(LivingFallEvent event) {
        if (SpellstoneSwordConfig.engineNullKinetic && holdsEngine(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!KineticDamage.isKinetic(event.getSource())) {
            return;
        }
        if (SpellstoneSwordConfig.engineNullKinetic && holdsEngine(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    /** 两手任一持有与失落引擎共鸣的共鸣者。抓钩虽是主手用的，切到副手也不该突然摔死。 */
    private static boolean holdsEngine(EntityLivingBase entity) {
        return isEngine(entity.getHeldItemMainhand()) || isEngine(entity.getHeldItemOffhand());
    }

    private static boolean isEngine(ItemStack stack) {
        // 原初共鸣常驻引擎的免疫，不必切到引擎形态
        return isSword(stack) && (SpellstoneData.isPrimevalActive(stack)
                || SpellstoneData.isResonatingWith(stack, SpellstoneForm.LOST_ENGINE));
    }

    // ============================ 击杀 / 暴击 ============================

    /**
     * 幻影击杀回满能量：近战打死，或这把剑射出的魂焰弹打死都算。
     * <p>魂焰弹的伤害是 {@code causeIndirectMagicDamage(弹体, 主人)}，直接来源是弹体、真正来源才是持剑者，
     * 所以不能只看直接来源（1.20.1 原版就是只看直接来源，魂焰弹击杀不回能）。
     * 饰品环绕的魂焰弹不算，免得戴着饰品又拿着剑时白送能量。炽焰的击杀炸岩浆按定稿已去掉。
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getTrueSource();
        if (!(killer instanceof EntityLivingBase)) {
            return;
        }
        Entity direct = event.getSource().getImmediateSource();
        boolean byFlameBall = direct instanceof EntitySoulFlameBall && ((EntitySoulFlameBall) direct).isShotBySword();
        if (direct != killer && !byFlameBall) {
            return;
        }
        ItemStack weapon = ((EntityLivingBase) killer).getHeldItemMainhand();
        if (isSword(weapon) && SpellstoneData.isResonatingWith(weapon, SpellstoneForm.ILLUSION_LANTERN)) {
            SpellstoneData.setEnergy(weapon, SpellstoneData.getMaxEnergy(weapon));
        }
    }

    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack weapon = player.getHeldItemMainhand();
        if (!isSword(weapon) || !event.isVanillaCritical()) {
            return;
        }
        if (!(event.getTarget() instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        int level = SpellstoneData.getLevel(weapon);

        if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.VOID_PEARL)) {
            int energy = SpellstoneData.getEnergy(weapon);
            if (energy > 0) {
                SpellstoneData.setEnergy(weapon, energy - 1);
                float rad = player.rotationYaw * 0.017453292F;
                target.knockBack(player, 0.4F, Math.sin(rad), -Math.cos(rad));
                event.setDamageModifier(event.getDamageModifier() + 1.5F);
            }
        } else if (SpellstoneData.isResonatingWith(weapon, SpellstoneForm.FORGOTTEN_ICE)) {
            // 暴击按目标冻结程度增伤。满级的群体冻结挪到了命中时（见 SpellstoneAbilities）
            event.setDamageModifier(event.getDamageModifier()
                    + FrostHelper.getFrostTicks(target) / (float) SpellstoneFormConfig.frostCritDivisor);
        }
    }

    /** 推进炽热计数（烈焰满级用）。 */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!event.getEntityLiving().world.isRemote) {
            HeatHelper.tick(event.getEntityLiving());
        }
    }

    // ============================ 各形态的命中细节 ============================

    /** 虚空追击：魔法伤补刀；满级清除目标全部增益并按层数追伤。 */
    private static void voidFollowUp(EntityLivingBase target, EntityLivingBase attacker, ItemStack weapon, float base) {
        int level = SpellstoneData.getLevel(weapon);
        target.hurtResistantTime = 0;
        target.attackEntityFrom(SpellstoneDamage.magic(attacker), base * (0.2F + level * 0.05F));
        if (level <= 4) {
            return;
        }
        int count = 0;
        for (PotionEffect effect : new ArrayList<>(target.getActivePotionEffects())) {
            if (effect.getPotion().isBadEffect()) {
                continue;
            }
            count += effect.getAmplifier() + 1;
            target.removePotionEffect(effect.getPotion());
        }
        if (count > 0) {
            target.hurtResistantTime = 0;
            target.attackEntityFrom(SpellstoneDamage.magic(attacker), base * count * (float) SpellstoneFormConfig.voidPurgeRatio);
        }
    }

    /** 复苏命中：概率叠毒并按毒层攒能量；满级对亡灵追加魔法伤，对中毒的活物也追加（原设计是给活物回血）。 */
    private static void revivalOnHit(EntityLivingBase target, EntityLivingBase attacker, ItemStack weapon, float damage) {
        int level = SpellstoneData.getLevel(weapon);
        if (attacker.getRNG().nextInt(100) < 30 + level * 4) {
            PotionEffect poison = target.getActivePotionEffect(MobEffects.POISON);
            int amp = poison == null ? 0 : Math.min(4, poison.getAmplifier() + 1);
            int dur = poison == null ? 400 : Math.max(240, poison.getDuration() + 60);
            target.addPotionEffect(new PotionEffect(MobEffects.POISON, dur, amp, true, true));
        }
        PotionEffect poison = target.getActivePotionEffect(MobEffects.POISON);
        if (poison != null) {
            // 不再乘攻击蓄力：命中事件触发时冷却已被重置，getCooledAttackStrength 恒为 0，
            // 乘上去就永远攒不到能量
            SpellstoneData.addEnergy(weapon, poison.getAmplifier() + 1);
        }
        if (level > 4) {
            float ratio = target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD
                    ? (float) SpellstoneFormConfig.revivalUndeadRatio
                    : poison != null ? (float) SpellstoneFormConfig.revivalPoisonedRatio : 0.0F;
            if (ratio > 0.0F) {
                target.hurtResistantTime = 0;
                target.attackEntityFrom(SpellstoneDamage.magic(attacker), damage * ratio);
            }
        }
    }

    /** 幻影命中：低血按魂等级斩杀追伤（斩杀线与追伤都随最大生命缩放）、叠魂等级；满级召唤周围不死围攻该目标。 */
    private static void illusionOnHit(EntityLivingBase target, EntityLivingBase attacker,
                                      ItemStack weapon, LivingDamageEvent event) {
        int level = SpellstoneData.getLevel(weapon);
        int soul = target.getEntityData().getInteger("IllusionSoulLevel");
        float maxHealth = target.getMaxHealth();
        float line = Math.max(soul * 2.0F, maxHealth * (float) Math.min(soul * SpellstoneFormConfig.illusionExecutePerSoul,
                SpellstoneFormConfig.illusionExecuteCap));
        if (soul > 0 && target.getHealth() < line + event.getAmount()) {
            event.setAmount(event.getAmount()
                    + Math.max(5.0F * soul, maxHealth * (float) SpellstoneFormConfig.illusionExecuteBonus));
        }
        target.getEntityData().setInteger("IllusionSoulLevel", soul + (level + 1) / 2);
        if (level <= 4) {
            return;
        }
        float w = target.width * 4.0F;
        List<EntityLiving> mobs = target.world.getEntitiesWithinAABB(EntityLiving.class,
                target.getEntityBoundingBox().grow(w, target.height / 2.0F, w));
        for (EntityLiving mob : mobs) {
            if (mob == target || mob.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD) {
                continue;
            }
            if ((mob.getAttackTarget() == null || mob.getAttackTarget() == attacker)
                    && mob.getRevengeTarget() != attacker && target instanceof EntityLivingBase) {
                mob.setAttackTarget(target);
            }
        }
    }

    // ============================ 判定工具 ============================

    private static boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemSpellstoneSword;
    }

    /** 近战判定：直接来源就是攻击者本人，且非弹射物、非爆炸、非穿甲。 */
    static boolean isMelee(DamageSource source) {
        return source.getImmediateSource() instanceof EntityLivingBase
                && source.getTrueSource() == source.getImmediateSource()
                && !source.isProjectile() && !source.isExplosion() && !source.isUnblockable();
    }
}
