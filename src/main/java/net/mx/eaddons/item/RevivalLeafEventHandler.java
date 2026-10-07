package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 复苏之叶的伤害/治疗联动，移植自 1.20 神遗拓展 AddonEventHandler 中与叶子相关的片段。
 * 1) 佩戴者攻击命中时给目标施加中毒；
 * 2) 被打上 RevivingPoisoned 标记（靠近中毒生物）的实体治疗量降至 25%；
 * 3) 佩戴者无缝免疫 饥饿/中毒/凋零（施加即拒绝）。
 */
public class RevivalLeafEventHandler {

    /**
     * 无缝免疫 饥饿/中毒/凋零：在效果施加入口直接 DENY（对齐丰富饰品"牛黄"的做法）。
     * 原先只在佩戴 tick 里移除效果，高等级中毒会在"施加→下个 tick 移除"的间隙反复结算伤害；
     * 拒绝施加后效果根本不存在，不再掉血。戴上前已有的效果仍由 onWornTick 的清除逻辑兜底。
     */
    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        Potion potion = event.getPotionEffect().getPotion();
        if (potion != MobEffects.HUNGER && potion != MobEffects.POISON && potion != MobEffects.WITHER) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (BaublesApi.isBaubleEquipped(player, ItemRevivalLeaf.INSTANCE) == -1) return;
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayer)) return;
        EntityPlayer attacker = (EntityPlayer) source;
        if (attacker.world.isRemote) return;
        if (BaublesApi.isBaubleEquipped(attacker, ItemRevivalLeaf.INSTANCE) == -1) return;

        EntityLivingBase victim = event.getEntityLiving();
        victim.addPotionEffect(new PotionEffect(MobEffects.POISON,
                RevivalLeafConfig.poisonTime, RevivalLeafConfig.poisonLevel, false, true));
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntityLiving().getEntityData().getBoolean("RevivingPoisoned")) {
            event.setAmount(event.getAmount() * 0.25F);
        }
    }

    /** 目标不再中毒时清除 RevivingPoisoned 标记。 */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        if (entity.getEntityData().getBoolean("RevivingPoisoned")
                && !entity.isPotionActive(MobEffects.POISON)) {
            entity.getEntityData().removeTag("RevivingPoisoned");
        }
    }
}
