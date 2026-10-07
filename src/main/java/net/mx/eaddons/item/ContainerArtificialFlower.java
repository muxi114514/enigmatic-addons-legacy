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
import net.minecraft.util.text.TextComponentTranslation;
import net.mx.eaddons.flower.FlowerCoreChoice;
import net.mx.eaddons.flower.FlowerMaterial;

import java.util.UUID;

public class ContainerArtificialFlower extends Container {
    private static final int QUARTZ_COST = 4;
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

        this.addSlotToContainer(new SlotMaterial(this.enchantSlot, Items.DYE, 4, 0, 17, 31));
        this.addSlotToContainer(new SlotMaterial(this.enchantSlot, Items.QUARTZ, -1, 1, 106, 31));
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
        // 栏里放的是术质核心时走自选（FlowerChoiceMessage），不能当青金石 / 石英随机洗练
        if ((id >= 0 && id < 3 && hasCore(0)) || ((id == 4 || id == 5) && hasCore(1))) {
            return false;
        }
        if (id >= 0 && id < 3) {
            // 邪恶精髓：固定消耗、按最高档分布、上限更高；青金石按所选档位
            boolean essence = hasEssence(0);
            if (!enough(0) && !player.capabilities.isCreativeMode) return false;

            if (!player.world.isRemote) {
                boolean rolled = ItemArtificialFlower.Helper.randomAttribute(player,
                        ItemArtificialFlower.Helper.getFlowerStack(player, false),
                        id + 1, essence ? 2 : this.costMode, !this.ringSlot.getStackInSlot(0).isEmpty(),
                        attributeCap(0));
                if (rolled) {
                    consume(player, 0, materialCost(0));
                }
            }
            return true;
        } else if (id == 3) {
            this.costMode = this.costMode == 2 ? 0 : this.costMode + 1;
            return true;
        } else if (id == 4 || id == 5) {
            // 免疫只认石英；常驻用石英抽基础名单，用邪恶精髓抽名单以外的
            boolean essence = hasEssence(1);
            if (essence && id == 5) return false;
            if (!enough(1) && !player.capabilities.isCreativeMode) return false;

            if (!player.world.isRemote) {
                boolean rolled = ItemArtificialFlower.Helper.randomEffect(player,
                        ItemArtificialFlower.Helper.getFlowerStack(player, false), id - 4, essence);
                if (rolled) {
                    consume(player, 1, materialCost(1));
                } else {
                    player.sendMessage(new TextComponentTranslation("msg.eaddons.artificial_flower.no_candidate"));
                }
            }
            return true;
        }
        return false;
    }

    private void consume(EntityPlayer player, int index, int cost) {
        player.world.playSound(null, player.getPosition(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                1.0F, player.world.rand.nextFloat() * 0.1F + 0.9F);
        if (!player.capabilities.isCreativeMode) {
            ItemStack material = this.enchantSlot.getStackInSlot(index);
            material.shrink(cost);
            if (material.isEmpty()) this.enchantSlot.setInventorySlotContents(index, ItemStack.EMPTY);
        }
    }

    /** 材料栏（0 属性、1 效果）里放的是邪恶精髓。 */
    public boolean hasEssence(int index) {
        return (index == 0 || index == 1) && FlowerMaterial.isEssence(this.enchantSlot.getStackInSlot(index));
    }

    /** 这一栏当前材料每次洗练的消耗：邪恶精髓按配置，青金石按档位 2/4/8，石英 4。 */
    public int materialCost(int index) {
        if (hasEssence(index)) return ArtificialFlowerConfig.evilEssenceCost;
        if (index == 0) return this.costMode == 0 ? 2 : this.costMode == 1 ? 4 : 8;
        return QUARTZ_COST;
    }

    /** 属性洗练上限（%）：邪恶精髓用高上限，青金石用低上限。 */
    public int attributeCap(int index) {
        return hasEssence(index) ? ArtificialFlowerConfig.randomAttributeMaxModifier
                : ArtificialFlowerConfig.lapisAttributeMaxModifier;
    }

    private boolean enough(int index) {
        ItemStack material = this.enchantSlot.getStackInSlot(index);
        return !material.isEmpty() && material.getCount() >= materialCost(index);
    }

    /** 属性栏（0）/ 常驻效果（1）当前能否洗练，供界面点亮按钮。 */
    public boolean valid(int index) {
        if (index > 1 || hasCore(index)) return false;
        return enough(index);
    }

    /** 免疫效果当前能否洗练：只认石英。 */
    public boolean validImmunity() {
        return !hasCore(1) && !hasEssence(1) && enough(1);
    }

    public boolean hasRing() {
        return !this.ringSlot.getStackInSlot(0).isEmpty();
    }

    /** 材料栏（0 青金石栏、1 石英栏）里放的是术质核心。 */
    public boolean hasCore(int index) {
        return (index == 0 || index == 1) && FlowerCoreChoice.isCore(this.enchantSlot.getStackInSlot(index));
    }

    /** 自选成功后扣 1 个核心（创造模式不扣）。 */
    public void consumeCore(int index) {
        if (this.player.capabilities.isCreativeMode) {
            return;
        }
        ItemStack core = this.enchantSlot.getStackInSlot(index);
        core.shrink(1);
        if (core.isEmpty()) {
            this.enchantSlot.setInventorySlotContents(index, ItemStack.EMPTY);
        }
        this.detectAndSendChanges();
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

    /** 材料栏：原有材料之外还接受术质核心（彩蛋：放入后洗练按钮改为自选）。 */
    public static class SlotMaterial extends SlotExact {
        public SlotMaterial(IInventory inventoryIn, Item item, int meta, int index, int x, int y) {
            super(inventoryIn, item, meta, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return super.isItemValid(stack) || FlowerCoreChoice.isCore(stack) || FlowerMaterial.isEssence(stack);
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
