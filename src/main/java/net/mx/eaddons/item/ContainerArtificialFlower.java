package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;

import java.util.UUID;

public class ContainerArtificialFlower extends Container {
    public final EntityPlayer player;
    private final IInventory enchantSlot;
    private final IInventory ringSlot;
    public int costMode;

    public ContainerArtificialFlower(EntityPlayer player) {
        this.player = player;
        this.costMode = 0;
        this.enchantSlot = new InventoryBasic("Enchant", false, 2);
        this.ringSlot = new InventoryBasic("Ring", false, 1);

        ItemStack flowerStack = ItemArtificialFlower.Helper.getFlowerStack(player, true);
        NBTTagCompound flowerTag = flowerStack.getTagCompound();
        if (flowerTag != null && flowerTag.hasKey("MagicRing")) {
            NBTTagCompound ringNBT = flowerTag.getCompoundTag("MagicRing");
            ringSlot.setInventorySlotContents(0, new ItemStack(ringNBT));
        }

        this.addSlotToContainer(new SlotExact(this.enchantSlot, Items.DYE, 4, 0, 17, 31));
        this.addSlotToContainer(new SlotExact(this.enchantSlot, Items.QUARTZ, -1, 1, 106, 31));
        this.addSlotToContainer(new SlotRing(this.ringSlot, 0, 80, 27));

        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(player.inventory, j + k * 9 + 9, 8 + j * 18, 84 + k * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            if (k == player.inventory.currentItem) {
                this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 142) {
                    @Override
                    public boolean canTakeStack(EntityPlayer playerIn) {
                        return false;
                    }

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public int getSlotStackLimit() {
                        return 0;
                    }
                });
            } else {
                this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 142));
            }
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id >= 0 && id < 3) {
            ItemStack lapis = this.enchantSlot.getStackInSlot(0);
            if (lapis.isEmpty() && !player.capabilities.isCreativeMode) return false;
            int cost = this.costMode == 0 ? 2 : this.costMode == 1 ? 4 : 8;
            if (lapis.getCount() < cost && !player.capabilities.isCreativeMode) return false;

            if (!player.world.isRemote) {
                ItemArtificialFlower.Helper.randomAttribute(player,
                        ItemArtificialFlower.Helper.getFlowerStack(player, false),
                        id + 1, this.costMode, !this.ringSlot.getStackInSlot(0).isEmpty());
                player.world.playSound(null, player.getPosition(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                        1.0F, player.world.rand.nextFloat() * 0.1F + 0.9F);
                if (!player.capabilities.isCreativeMode) {
                    lapis.shrink(cost);
                    if (lapis.isEmpty()) this.enchantSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                }
            }
            return true;
        } else if (id == 3) {
            this.costMode = this.costMode == 2 ? 0 : this.costMode + 1;
            return true;
        } else if (id == 4 || id == 5) {
            ItemStack quartz = this.enchantSlot.getStackInSlot(1);
            if (quartz.isEmpty() && !player.capabilities.isCreativeMode) return false;
            if (quartz.getCount() < 4 && !player.capabilities.isCreativeMode) return false;

            if (!player.world.isRemote) {
                ItemArtificialFlower.Helper.randomEffect(player,
                        ItemArtificialFlower.Helper.getFlowerStack(player, false),
                        id - 4);
                player.world.playSound(null, player.getPosition(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                        1.0F, player.world.rand.nextFloat() * 0.1F + 0.9F);
                if (!player.capabilities.isCreativeMode) {
                    quartz.shrink(4);
                    if (quartz.isEmpty()) this.enchantSlot.setInventorySlotContents(1, ItemStack.EMPTY);
                }
            }
            return true;
        }
        return false;
    }

    public boolean valid(int index) {
        if (index > 1) return false;
        boolean[] flag = {
                this.enchantSlot.getStackInSlot(0).getCount() >= (this.costMode == 0 ? 2 : this.costMode == 1 ? 4 : 8),
                this.enchantSlot.getStackInSlot(1).getCount() >= 4
        };
        return !this.enchantSlot.getStackInSlot(index).isEmpty() && flag[index];
    }

    public boolean hasRing() {
        return !this.ringSlot.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack result = ItemStack.EMPTY;
        int slotsCount = 3;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotItem = slot.getStack();
            result = slotItem.copy();
            if (index < slotsCount) {
                if (!this.mergeItemStack(slotItem, slotsCount, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(slotItem, 0, slotsCount, false)) {
                return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (slotItem.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, slotItem);
        }
        return result;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (player instanceof EntityPlayerMP) {
            for (int i = 0; i < 2; i++) {
                ItemStack stack = this.enchantSlot.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (player.isEntityAlive() && !((EntityPlayerMP) player).hasDisconnected()) {
                        player.inventory.placeItemBackInInventory(player.world, stack);
                    } else {
                        player.dropItem(stack, false);
                    }
                    this.enchantSlot.setInventorySlotContents(i, ItemStack.EMPTY);
                }
            }

            ItemStack flowerStack = ItemArtificialFlower.Helper.getFlowerStack(player, false);
            if (!flowerStack.isEmpty()) {
                NBTTagCompound tag = flowerStack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    flowerStack.setTagCompound(tag);
                }
                UUID uuid = UUID.randomUUID();
                ItemArtificialFlower.Helper.setPlayerEnableUUID(player, uuid);
                tag.setUniqueId("FlowerUUID", uuid);
                tag.setBoolean("FlowerEnable", true);

                ItemStack ring = this.ringSlot.getStackInSlot(0);
                if (!ring.isEmpty()) {
                    NBTTagCompound ringTag = ring.writeToNBT(new NBTTagCompound());
                    tag.setTag("MagicRing", ringTag);
                } else {
                    tag.removeTag("MagicRing");
                }
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /**
     * Slot that only accepts a specific item. For lapis lazuli (dye meta=4) or quartz.
     */
    public static class SlotExact extends Slot {
        private final Item acceptedItem;
        private final int acceptedMeta;

        public SlotExact(IInventory inventoryIn, Item item, int meta, int index, int x, int y) {
            super(inventoryIn, index, x, y);
            this.acceptedItem = item;
            this.acceptedMeta = meta;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            if (acceptedMeta >= 0) {
                return stack.getItem() == acceptedItem && stack.getMetadata() == acceptedMeta;
            }
            return stack.getItem() == acceptedItem;
        }
    }

    public static class SlotRing extends Slot {
        public SlotRing(IInventory inventoryIn, int index, int x, int y) {
            super(inventoryIn, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.getItem() instanceof ItemQuartzRing;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
