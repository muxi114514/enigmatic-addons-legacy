package net.mx.eaddons.item;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemIchorDroplet extends Item {
    public static final ItemIchorDroplet INSTANCE = new ItemIchorDroplet();

    public ItemIchorDroplet() {
        setMaxDamage(0);
        maxStackSize = 64;
        setUnlocalizedName("ichor_droplet");
        setRegistryName("ichor_droplet");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }
}
