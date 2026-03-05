package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerAntiqueBag extends Container {
    private final InventoryAntiqueBag bagInventory;
    private final EntityPlayer player;

    public ContainerAntiqueBag(EntityPlayer player) {
        this.player = player;
        this.bagInventory = new InventoryAntiqueBag(player);

        // Bag slots: 2 rows x 6 columns = 12 slots
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                this.addSlotToContainer(new SlotAntiqueBag(bagInventory, col + row * 6,
                        35 + col * 18, 24 + row * 18));
            }
        }

        // Player inventory slots (3 rows x 9 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar slots (1 row x 9 columns)
        for (int col = 0; col < 9; col++) {
            if (col == player.inventory.currentItem) {
                // Lock the currently selected slot if holding the bag
                this.addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 142) {
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
                this.addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 142));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack resultStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            resultStack = slotStack.copy();

            int bagSlotCount = ItemAntiqueBag.SLOT_COUNT;

            if (index < bagSlotCount) {
                // Transfer from bag to player inventory
                if (!this.mergeItemStack(slotStack, bagSlotCount, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Transfer from player inventory to bag
                if (!this.mergeItemStack(slotStack, 0, bagSlotCount, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == resultStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return resultStack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        bagInventory.closeInventory(playerIn);
    }

    /**
     * Custom slot that only accepts allowed items.
     * Flowers (artificial_flower) are only allowed in the first 2 slots (index 0 and 1).
     * Matches high-version logic: (isBook(stack) || (isFlower && index < 2))
     */
    public static class SlotAntiqueBag extends Slot {
        private final int bagSlotIndex;

        public SlotAntiqueBag(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
            this.bagSlotIndex = index;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            boolean isFlowerInFirstTwo = ItemAntiqueBag.isFlower(stack) && bagSlotIndex < 2;
            return ItemAntiqueBag.isBook(stack) || isFlowerInFirstTwo;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
