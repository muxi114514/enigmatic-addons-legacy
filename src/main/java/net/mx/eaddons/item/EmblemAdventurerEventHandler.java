package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Optional;

/**
 * In Hardcore worlds, replace any Emblem of Adventurer (in inventory, off-hand, or Baubles) with Insignia of Despair.
 * Runs every server tick so it works as soon as the item is in the player's inventory.
 */
public class EmblemAdventurerEventHandler {

    @SubscribeEvent
    @Optional.Method(modid = "baubles")
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        if (player.world == null || !player.world.getWorldInfo().isHardcoreModeEnabled()) return;

        ItemStack insignia = new ItemStack(ItemInsigniaOfDespair.INSTANCE);

        // Main inventory (hotbar + main grid)
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ItemEmblemOfAdventurer.INSTANCE) {
                player.inventory.setInventorySlotContents(i, insignia.copy());
            }
        }

        // Off-hand
        ItemStack off = player.getHeldItemOffhand();
        if (!off.isEmpty() && off.getItem() == ItemEmblemOfAdventurer.INSTANCE) {
            player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, insignia.copy());
        }

        // Baubles slots
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack inSlot = handler.getStackInSlot(i);
                if (!inSlot.isEmpty() && inSlot.getItem() == ItemEmblemOfAdventurer.INSTANCE) {
                    handler.setStackInSlot(i, insignia.copy());
                }
            }
        }
    }
}
