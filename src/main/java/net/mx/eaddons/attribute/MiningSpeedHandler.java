package net.mx.eaddons.attribute;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 挖掘加速：与 EL 原挖掘加成同一时机（LOWEST），按原始速度加成，两端都算 */
public class MiningSpeedHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        double bonus = EnigmaticAttributes.get(event.getEntityPlayer(), EnigmaticAttributes.MINING_SPEED);
        if (bonus > 0) {
            event.setNewSpeed(event.getNewSpeed() + (float) (event.getOriginalSpeed() * bonus));
        }
    }
}
