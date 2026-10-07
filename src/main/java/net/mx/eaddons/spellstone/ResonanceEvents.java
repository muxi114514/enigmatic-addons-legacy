package net.mx.eaddons.spellstone;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.mx.eaddons.compat.ModCompat;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneFormConfig;

/**
 * 两个立方形态的事件层：非欧的伤害结算、非欧领域的瞬移拦截，
 * 以及两个形态共有的「抵消禁忌之果负面」。
 *
 * <p>地狱刃片护符那条豁免写在饰品自己的代码里（负面就是在那里施加的），不在本类。
 */
public class ResonanceEvents {

    // ============================ 非欧：伤害结算 ============================

    /**
     * 护甲前：随机倍率与负面加成一次算完，再按比例扣下「虚空」那一份，暂存到护甲结算之后原样加回，
     * 等效于这一份无视护甲、保护附魔与抗性药水。不另打一次伤害，免得嵌套结算里目标死两次。
     * 装了 First Aid 的玩家受害者不触发护甲后的事件，不拆分。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        float amount = event.getAmount();
        EntityLivingBase attacker = cubeAttacker(event.getSource());
        if (attacker == null || amount <= 0.0F || amount >= Float.MAX_VALUE) {
            return;
        }
        EntityLivingBase target = event.getEntityLiving();
        amount *= CubeResonance.rollMultiplier(attacker);
        amount *= 1.0F + CubeResonance.debuffBonus(target);
        float voidPart = ModCompat.firstAidTakesOver(target)
                ? 0.0F : amount * (float) SpellstoneFormConfig.cubeVoidRatio;
        event.setAmount(amount - voidPart);
        CubeResonance.holdVoidPart(target, event.getSource(), voidPart);
    }

    /** 护甲后：加回虚空那一份，再按概率追加「削去当前生命一半」（放在护甲后，不被护甲削弱）。 */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        EntityLivingBase target = event.getEntityLiving();
        float amount = event.getAmount() + CubeResonance.takeVoidPart(target, event.getSource());
        EntityLivingBase attacker = cubeAttacker(event.getSource());
        if (attacker != null) {
            amount += CubeResonance.paradoxStrike(target, attacker);
        }
        event.setAmount(amount);
    }

    /** 近战且主手是非欧共鸣中的共鸣者时返回攻击者；弹射物与魔法伤不吃非欧的结算。 */
    private static EntityLivingBase cubeAttacker(DamageSource source) {
        Entity trueSource = source.getTrueSource();
        if (!(trueSource instanceof EntityLivingBase) || source.getImmediateSource() != trueSource) {
            return null;
        }
        ItemStack weapon = ((EntityLivingBase) trueSource).getHeldItemMainhand();
        return weapon.getItem() instanceof ItemSpellstoneSword && SpellstoneData.isCubeActive(weapon)
                ? (EntityLivingBase) trueSource : null;
    }

    // ============================ 抵消禁忌之果的负面 ============================

    /**
     * EL 让吃过禁忌之果的玩家「单次 ≤1 点的治疗（自然回血）只剩 {@code 1 - regenerationSubtraction}」，
     * 判定在 {@code EnigmaticEvents#onLivingHeal}（默认优先级）。这里排在它之后按同一比例除回去，
     * 顺带把千咒卷轴那部分加成一并还原——EL 的加成是基于已削减的数值算的，除法正好抵消。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getAmount() > 1.0F) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !ResonanceLink.holdsActiveLink(player)) {
            return;
        }
        float remain = 1.0F - EnigmaticConfigs.regenerationSubtraction;
        if (remain <= 0.0F || remain >= 1.0F || !IForbiddenConsumed.get(player).isConsumed()) {
            return;
        }
        event.setAmount(event.getAmount() / remain);
    }

    // ============================ 非欧领域：禁止瞬移 ============================

    /**
     * 原版末影人、潜影贝与末影珍珠的瞬移都走这个事件，取消即可。
     * Infernal Mobs 的 Ender 词条自写瞬移、不发事件，另由 {@code MixinInfernalEnder} 拦。
     */
    @SubscribeEvent
    public void onEnderTeleport(EnderTeleportEvent event) {
        if (CubeResonance.domainBlocks(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }
}
