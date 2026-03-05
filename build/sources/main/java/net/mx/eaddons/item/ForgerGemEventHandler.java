package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketWindowProperty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.WeakHashMap;

public class ForgerGemEventHandler {

    private final Map<EntityPlayer, Integer> lastSetCost = new WeakHashMap<>();

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        EntityPlayer player = findAnvilPlayer(event);
        if (player == null) return;
        if (!ItemForgerGem.hasForgerGem(player)) return;
        if (!ItemForgerGem.hasRingEquipped(player)) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!qualifiesForUnbreakable(player, left, right)) return;

        ItemStack result = left.copy();
        NBTTagCompound existingTag = result.getTagCompound();
        NBTTagCompound tag = existingTag != null ? existingTag.copy() : new NBTTagCompound();
        tag.setBoolean("Unbreakable", true);
        result.setTagCompound(tag);
        result.setRepairCost(result.getRepairCost() + 8);

        String name = event.getName();
        if (name != null && !name.isEmpty()) {
            result.setStackDisplayName(name);
        }

        event.setOutput(result);
        event.setCost(30);
    }

    @SubscribeEvent
    public void onAnvilRepair(AnvilRepairEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != null && !player.world.isRemote && ItemForgerGem.hasForgerGem(player)) {
            event.setBreakChance(0F);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;

        if (!ItemForgerGem.hasForgerGem(player)) {
            lastSetCost.remove(player);
            return;
        }

        if (!(player.openContainer instanceof ContainerRepair)) {
            lastSetCost.remove(player);
            return;
        }

        ContainerRepair anvil = (ContainerRepair) player.openContainer;
        int currentCost = anvil.maximumCost;

        if (currentCost <= 0) {
            lastSetCost.remove(player);
            return;
        }

        ItemStack output = anvil.getSlot(2).getStack();
        NBTTagCompound outputTag = output.getTagCompound();
        if (!output.isEmpty() && outputTag != null && outputTag.getBoolean("Unbreakable")) {
            lastSetCost.remove(player);
            return;
        }

        Integer lastSet = lastSetCost.get(player);
        if (lastSet == null || currentCost != lastSet) {
            int halved = (currentCost + 1) / 2;
            anvil.maximumCost = halved;
            lastSetCost.put(player, halved);

            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).connection.sendPacket(
                        new SPacketWindowProperty(anvil.windowId, 0, halved)
                );
            }
        }
    }

    /**
     * Finds the player whose open anvil container holds the same left input stack (by reference)
     * as the event. Works on both server (via player list) and client (via ForgerGemClientHelper).
     */
    private EntityPlayer findAnvilPlayer(AnvilUpdateEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            ItemStack eventLeft = event.getLeft();
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (player.openContainer instanceof ContainerRepair) {
                    ContainerRepair anvil = (ContainerRepair) player.openContainer;
                    if (anvil.getSlot(0).getStack() == eventLeft) {
                        return player;
                    }
                }
            }
            return null;
        }

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            return ForgerGemClientHelper.findPlayerForAnvil(event.getLeft());
        }
        return null;
    }

    private boolean qualifiesForUnbreakable(EntityPlayer player, ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        if (left.getItem() != right.getItem()) return false;
        if (!left.isItemStackDamageable()) return false;
        if (left.getItemDamage() != 0 || right.getItemDamage() != 0) return false;
        if (left.isItemEnchanted() || right.isItemEnchanted()) return false;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(left.getItem());
        if (itemId != null && ForgerGemConfig.isBlacklisted(itemId)) return false;

        if (ForgerGemConfig.strictUnbreakableForge && !player.capabilities.isCreativeMode) {
            Item item = left.getItem();
            if (!(item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemArmor)) return false;
        }

        return true;
    }
}
