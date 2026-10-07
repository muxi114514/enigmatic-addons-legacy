package net.mx.eaddons.client;

import keletu.enigmaticlegacy.util.compat.CompatBaublesEX;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.mx.eaddons.baubleslot.ElSlotUnlocks;

/** 既没有 BaublesEX 也没有 BaubleVault 时，在天体果实、灵液瓶的提示里注明不会增加饰品栏 */
public class SlotItemTooltipHandler {

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if ((item == CompatBaublesEX.astralFruit || item == CompatBaublesEX.ichorBottle)
                && !ElSlotUnlocks.slotUnlockAvailable()) {
            event.getToolTip().add(I18n.format("tooltip.eaddons.noSlotUnlock"));
        }
    }
}
