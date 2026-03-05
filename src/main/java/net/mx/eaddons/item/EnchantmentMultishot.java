package net.mx.eaddons.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

public class EnchantmentMultishot extends Enchantment {
    public static final EnchantmentMultishot INSTANCE = new EnchantmentMultishot();

    public EnchantmentMultishot() {
        super(Rarity.RARE, EnumEnchantmentType.BOW, new EntityEquipmentSlot[] { EntityEquipmentSlot.MAINHAND });
        setName("eaddons.multishot");
        setRegistryName("eaddons", "multishot");
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 20;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 50;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof ItemDragonBow;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof ItemDragonBow;
    }
}
