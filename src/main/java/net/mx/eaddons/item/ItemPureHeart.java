package net.mx.eaddons.item;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemPureHeart extends Item {
    public static final ItemPureHeart INSTANCE = new ItemPureHeart();

    public ItemPureHeart() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("pure_heart");
        setRegistryName("pure_heart");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }
}
