package net.mx.eaddons.primeval;

import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 原初立方（术石）的负面效果免疫——阻断层：负面效果根本加不上去，一次伤害都不会结算。
 * 清理层（戴上前已有的）在 {@link ItemPrimevalSpellstone#onWornTick}。
 */
public class PrimevalSpellstoneEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (event.getPotionEffect().getPotion().isBadEffect() && ItemPrimevalSpellstone.isWorn(event.getEntityLiving())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
