package net.mx.eaddons.elplus;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** 无特殊行为的材料：狱火余烬、以太粒、大地之心碎片 */
public class ItemElPlusMaterial extends Item {

    public static final ItemElPlusMaterial INFERNAL_CINDER = new ItemElPlusMaterial("infernal_cinder", EnumRarity.COMMON, 2000);
    public static final ItemElPlusMaterial ETHERIUM_NUGGET = new ItemElPlusMaterial("etherium_nugget", EnumRarity.UNCOMMON, 0);
    public static final ItemElPlusMaterial EARTH_HEART_FRAGMENT = new ItemElPlusMaterial("earth_heart_fragment", EnumRarity.UNCOMMON, 0);

    private final EnumRarity rarity;
    /** 作为燃料的燃烧时长（tick），0 = 不是燃料 */
    private final int burnTime;

    public ItemElPlusMaterial(String name, EnumRarity rarity, int burnTime) {
        this.rarity = rarity;
        this.burnTime = burnTime;
        setUnlocalizedName(name);
        setRegistryName(name);
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return rarity;
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return burnTime > 0 ? burnTime : -1;
    }
}
