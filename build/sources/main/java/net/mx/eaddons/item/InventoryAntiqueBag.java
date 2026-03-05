package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * An IInventory wrapper that reads/writes directly from/to the player's
 * persistent NBT data for the Antique Bag.
 */
public class InventoryAntiqueBag implements IInventory {
    private final EntityPlayer player;
    private NonNullList<ItemStack> inventory;

    public InventoryAntiqueBag(EntityPlayer player) {
        this.player = player;
        this.inventory = ItemAntiqueBag.getInventory(player);
    }

    @Override
    public int getSizeInventory() {
        return ItemAntiqueBag.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= getSizeInventory())
            return ItemStack.EMPTY;
        return inventory.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = getStackInSlot(index);
        if (!stack.isEmpty()) {
            ItemStack result;
            if (stack.getCount() <= count) {
                result = stack;
                inventory.set(index, ItemStack.EMPTY);
            } else {
                result = stack.splitStack(count);
                if (stack.getCount() == 0) {
                    inventory.set(index, ItemStack.EMPTY);
                }
            }
            markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = getStackInSlot(index);
        if (!stack.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
            markDirty();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < getSizeInventory()) {
            inventory.set(index, stack);
            markDirty();
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        ItemAntiqueBag.setInventory(player, inventory);
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        this.inventory = ItemAntiqueBag.getInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (ItemAntiqueBag.isFlower(stack)) {
            return index < 2;
        }
        return ItemAntiqueBag.isBook(stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < getSizeInventory(); i++) {
            inventory.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public String getName() {
        return "container.eaddons.antique_bag";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }
}
