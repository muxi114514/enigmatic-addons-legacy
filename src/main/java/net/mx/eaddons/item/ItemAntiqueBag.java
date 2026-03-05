package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.mx.eaddons.EAddonsMod;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import java.util.Arrays;
import java.util.List;

public class ItemAntiqueBag extends Item {
    public static final ItemAntiqueBag INSTANCE = new ItemAntiqueBag();
    public static final int SLOT_COUNT = 12;
    private static final String NBT_TAG = "JMHeavenAntiqueBag";

    /**
     * List of allowed item registry names that can be placed in the bag.
     */
    public static final List<ResourceLocation> ALLOWED_ITEMS = Arrays.asList(
            new ResourceLocation("enigmaticlegacy", "the_acknowledgment"),
            new ResourceLocation("enigmaticlegacy", "the_twist"),
            new ResourceLocation("enigmaticlegacy", "the_infinitum"),
            new ResourceLocation("enigmaticlegacy", "half_heart_mask"));

    public ItemAntiqueBag() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("antique_bag");
        setRegistryName("antique_bag");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            player.openGui(EAddonsMod.instance, AntiqueBagGuiHandler.GUI_ID, world,
                    (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    /**
     * Check if an item is a "book" (legacy relic) that goes in any bag slot.
     */
    public static boolean isBook(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        ResourceLocation regName = stack.getItem().getRegistryName();
        return regName != null && ALLOWED_ITEMS.contains(regName);
    }

    /**
     * Check if an item is allowed to be placed in the bag (book or flower).
     */
    public static boolean isAllowedItem(ItemStack stack) {
        return isBook(stack) || isFlower(stack);
    }

    /**
     * Read the bag inventory from the player's persistent NBT data.
     */
    public static NonNullList<ItemStack> getInventory(EntityPlayer player) {
        NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        NBTTagCompound persistent = getPersistentData(player);

        if (persistent.hasKey(NBT_TAG)) {
            NBTTagList tagList = persistent.getTagList(NBT_TAG, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound tag = tagList.getCompoundTagAt(i);
                int slot = tag.getInteger("Slot");
                if (slot >= 0 && slot < SLOT_COUNT) {
                    inventory.set(slot, new ItemStack(tag.getCompoundTag("Item")));
                }
            }
        }
        return inventory;
    }

    /**
     * Write the bag inventory to the player's persistent NBT data.
     */
    public static void setInventory(EntityPlayer player, NonNullList<ItemStack> inventory) {
        NBTTagCompound persistent = getPersistentData(player);
        NBTTagList tagList = new NBTTagList();
        for (int i = 0; i < inventory.size(); i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Slot", i);
            tag.setTag("Item", inventory.get(i).writeToNBT(new NBTTagCompound()));
            tagList.appendTag(tag);
        }
        persistent.setTag(NBT_TAG, tagList);
    }

    private static NBTTagCompound getPersistentData(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    /**
     * Check if the player has the bag in their inventory or ender chest.
     */
    public static boolean hasBag(EntityPlayer player) {
        // Check main inventory
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ItemAntiqueBag)
                return true;
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack.getItem() instanceof ItemAntiqueBag)
                return true;
        }

        // Check ender chest
        InventoryEnderChest enderChest = player.getInventoryEnderChest();
        for (int i = 0; i < enderChest.getSizeInventory(); i++) {
            ItemStack stack = enderChest.getStackInSlot(i);
            if (stack.getItem() instanceof ItemAntiqueBag)
                return true;
        }

        return false;
    }

    /**
     * Check if a specific item (by registry name) exists in the bag inventory.
     * Returns true if at least one copy is found.
     */
    public static boolean hasItemInBag(EntityPlayer player, ResourceLocation itemId) {
        if (!hasBag(player))
            return false;
        NonNullList<ItemStack> inv = getInventory(player);
        for (ItemStack stack : inv) {
            if (!stack.isEmpty()) {
                ResourceLocation regName = stack.getItem().getRegistryName();
                if (regName != null && regName.equals(itemId))
                    return true;
            }
        }
        return false;
    }

    /**
     * Convenience method to directly check by item reference.
     */
    public static boolean hasItemInBag(EntityPlayer player, Item item) {
        ResourceLocation regName = item.getRegistryName();
        return regName != null && hasItemInBag(player, regName);
    }

    public static boolean isFlower(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemArtificialFlower;
    }

    /**
     * Ticks flowers stored in the first 2 bag slots, applying their effects.
     */
    public static void tickFlowersInBag(EntityPlayer player) {
        if (!hasBag(player))
            return;
        NonNullList<ItemStack> inv = getInventory(player);
        boolean changed = false;
        for (int i = 0; i < 2 && i < inv.size(); i++) {
            ItemStack stack = inv.get(i);
            if (isFlower(stack)) {
                stack.getItem().onUpdate(stack, player.world, player, i, false);
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    stack.setTagCompound(tag);
                }
                tag.setBoolean("FlowerEnable", false);
                tag.setInteger("FlowerBagEnable", i);
                changed = true;
            }
        }
        if (changed) {
            setInventory(player, inv);
        }
    }
}
