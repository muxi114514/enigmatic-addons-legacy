package net.mx.eaddons.item;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 灵魂火球占位物品：不可获取、不进创造栏，仅供 {@link EntitySoulFlameBall} 渲染其外观。
 * 对应 1.20 神遗拓展作为 ItemSupplier 渲染源的同名物品。
 */
public class ItemSoulFlameBall extends Item {
    public static final ItemSoulFlameBall INSTANCE = new ItemSoulFlameBall();

    public ItemSoulFlameBall() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("soul_flame_ball");
        setRegistryName("soul_flame_ball");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }
}
