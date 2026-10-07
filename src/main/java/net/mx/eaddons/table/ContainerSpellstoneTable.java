package net.mx.eaddons.table;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ItemSpellstoneDebris;
import net.mx.eaddons.spellstone.SpellstoneForm;

import javax.annotation.Nullable;

/**
 * 术石工作台容器。输入是本容器内的临时清单（关界面退还玩家，不持久存储），
 * 布局与 1.20.1 一致：0 残片 / 1 术核 / 2 术石 / 3~9 材料 / 10 结果 / 11~46 玩家背包。
 *
 * <p>两种模式：
 * <ul>
 *   <li><b>合成</b>：术核 + 残片 + 材料 → 匹配 {@link SpellstoneTableRecipes} → 出术石</li>
 *   <li><b>回收</b>：只有术石槽有东西、其余输入全空 → 出 {@value SpellstoneTableRecipes#RECYCLE_DEBRIS} 个残片</li>
 * </ul>
 */
public class ContainerSpellstoneTable extends Container {

    /** 七格材料围着术核摆，坐标取自 1.20.1。 */
    private static final int[][] INGREDIENT_POS = {
            {60, 55}, {53, 35}, {60, 15}, {80, 8}, {100, 15}, {107, 35}, {100, 55}
    };

    private static final int RESULT_SLOT = SpellstoneTableRecipe.CRAFT_SIZE;
    private static final int INV_START = RESULT_SLOT + 1;

    private final EntityPlayer player;
    private final InventoryBasic craftMatrix;
    private final InventoryCraftResult craftResult = new InventoryCraftResult();
    /** 当前匹配到的配方，null 表示没有或处于回收模式。 */
    @Nullable
    private SpellstoneTableRecipe current;
    private boolean recycling;

    public ContainerSpellstoneTable(EntityPlayer player) {
        this.player = player;
        this.craftMatrix = new InventoryBasic("SpellstoneTable", false, SpellstoneTableRecipe.CRAFT_SIZE) {
            @Override
            public void markDirty() {
                super.markDirty();
                ContainerSpellstoneTable.this.onCraftMatrixChanged(this);
            }
        };

        addSlotToContainer(new FilterSlot(craftMatrix, SpellstoneTableRecipe.SLOT_DEBRIS, 27, 35,
                stack -> stack.getItem() == ItemSpellstoneDebris.INSTANCE, 64));
        addSlotToContainer(new FilterSlot(craftMatrix, SpellstoneTableRecipe.SLOT_CORE, 80, 35,
                SpellstoneTableRecipes::isCoreItem, 1));
        addSlotToContainer(new FilterSlot(craftMatrix, SpellstoneTableRecipe.SLOT_SPELLSTONE, 133, 35,
                SpellstoneForm::isResonatable, 1));
        for (int i = 0; i < SpellstoneTableRecipe.INGREDIENT_COUNT; i++) {
            addSlotToContainer(new Slot(craftMatrix, SpellstoneTableRecipe.SLOT_INGREDIENT_START + i,
                    INGREDIENT_POS[i][0], INGREDIENT_POS[i][1]));
        }
        addSlotToContainer(new ResultSlot(craftResult, 0, 80, 62));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 142));
        }
    }

    // ============================ 结果刷新 ============================

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        if (this.player.world.isRemote) {
            return;
        }
        // 回收模式：只有术石槽有东西
        boolean stoneOnly = !this.craftMatrix.getStackInSlot(SpellstoneTableRecipe.SLOT_SPELLSTONE).isEmpty()
                && this.craftMatrix.getStackInSlot(SpellstoneTableRecipe.SLOT_DEBRIS).isEmpty()
                && this.craftMatrix.getStackInSlot(SpellstoneTableRecipe.SLOT_CORE).isEmpty()
                && noIngredients();
        if (stoneOnly) {
            this.recycling = true;
            this.current = null;
            this.craftResult.setInventorySlotContents(0,
                    new ItemStack(ItemSpellstoneDebris.INSTANCE, SpellstoneTableRecipes.RECYCLE_DEBRIS));
            return;
        }
        this.recycling = false;
        this.current = SpellstoneTableRecipes.find(this.craftMatrix);
        this.craftResult.setInventorySlotContents(0,
                this.current == null ? ItemStack.EMPTY : this.current.getResult().copy());
    }

    private boolean noIngredients() {
        for (int i = 0; i < SpellstoneTableRecipe.INGREDIENT_COUNT; i++) {
            if (!this.craftMatrix.getStackInSlot(SpellstoneTableRecipe.SLOT_INGREDIENT_START + i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 取走结果时扣掉对应的输入。 */
    private void consumeInputs() {
        if (this.recycling) {
            this.craftMatrix.setInventorySlotContents(SpellstoneTableRecipe.SLOT_SPELLSTONE, ItemStack.EMPTY);
            return;
        }
        if (this.current == null) {
            return;
        }
        this.craftMatrix.decrStackSize(SpellstoneTableRecipe.SLOT_DEBRIS, this.current.getDebrisCount());
        this.craftMatrix.setInventorySlotContents(SpellstoneTableRecipe.SLOT_CORE, ItemStack.EMPTY);
        for (int i = 0; i < SpellstoneTableRecipe.INGREDIENT_COUNT; i++) {
            this.craftMatrix.decrStackSize(SpellstoneTableRecipe.SLOT_INGREDIENT_START + i, 1);
        }
    }

    // ============================ 关界面退还 ============================

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (player.world.isRemote) {
            return;
        }
        for (int i = 0; i < SpellstoneTableRecipe.CRAFT_SIZE; i++) {
            ItemStack stack = this.craftMatrix.removeStackFromSlot(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, false);
            }
        }
        this.craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    // ============================ Shift 点击 ============================

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index == RESULT_SLOT) {
            if (!mergeItemStack(stack, INV_START, this.inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, copy);
        } else if (index < RESULT_SLOT) {
            // 输入槽 → 背包
            if (!mergeItemStack(stack, INV_START, this.inventorySlots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 背包 → 优先塞进对得上的专用槽，否则进材料槽
            if (!mergeItemStack(stack, 0, SpellstoneTableRecipe.CRAFT_SIZE, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    // ============================ 槽位 ============================

    /** 只接受特定物品的输入槽。 */
    private static class FilterSlot extends Slot {
        private final java.util.function.Predicate<ItemStack> filter;
        private final int limit;

        FilterSlot(IInventory inventory, int index, int x, int y,
                   java.util.function.Predicate<ItemStack> filter, int limit) {
            super(inventory, index, x, y);
            this.filter = filter;
            this.limit = limit;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return this.filter.test(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return this.limit;
        }
    }

    /** 结果槽：只能取出，取出时扣输入。 */
    private class ResultSlot extends Slot {
        ResultSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            ContainerSpellstoneTable.this.consumeInputs();
            ContainerSpellstoneTable.this.onCraftMatrixChanged(ContainerSpellstoneTable.this.craftMatrix);
            return super.onTake(player, stack);
        }
    }
}
