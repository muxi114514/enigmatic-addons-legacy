package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 焦阳护符的伤害联动，移植自 1.20 神遗拓展 AddonEventHandler 中与护符相关的片段：
 * 火焰伤害免疫、几率抗伤（岩浆中翻倍）、攻击着火目标吸血。
 */
public class ScorchedCharmEventHandler {

    /** 火焰免疫 + 几率抗伤。用 LivingAttackEvent（可取消，先于伤害结算）。 */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !ItemScorchedCharm.isWorn(player)) return;

        // 火焰类伤害完全免疫；SimpleDifficulty 的过热伤害同样免疫（按伤害类型字符串判断，无需硬依赖）
        String damageType = event.getSource().damageType;
        if (ItemScorchedCharm.isFireImmuneDamage(damageType) || "hyperthermia".equals(damageType)) {
            event.setCanceled(true);
            return;
        }

        // 几率抗伤，岩浆中（含站在岩浆表面）概率翻倍
        int percentage = ScorchedCharmConfig.resistanceProbability * (ItemScorchedCharm.isInOrOnLava(player) ? 2 : 1);
        if (player.getRNG().nextInt(100) < percentage) {
            event.setCanceled(true);
        }
    }

    /** 攻击着火目标时按伤害比例吸血。 */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Entity direct = event.getSource().getImmediateSource();
        if (!(direct instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) direct;
        if (player.world.isRemote || !ItemScorchedCharm.isWorn(player)) return;

        EntityLivingBase victim = event.getEntityLiving();
        if (victim.isBurning()) {
            float lifesteal = event.getAmount() * ScorchedCharmConfig.getLifestealModifier();
            if (lifesteal > 0) player.heal(lifesteal);
        }
    }
}
