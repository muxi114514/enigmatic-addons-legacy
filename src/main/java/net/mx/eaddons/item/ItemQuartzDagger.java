package net.mx.eaddons.item;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 魔法石英匕首：不可获取、不进创造栏，仅作为 {@link EntityQuartzDagger} 投射物渲染时持有的物品，
 * 用来复用原版 3D 物品渲染管线（对应 1.20 神遗拓展里同名的占位 Item）。
 */
public class ItemQuartzDagger extends Item {
    public static final ItemQuartzDagger INSTANCE = new ItemQuartzDagger();

    public ItemQuartzDagger() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("quartz_dagger");
        setRegistryName("quartz_dagger");
        // 不设置创造标签：这是内部渲染用物品，不应出现在创造栏
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }
}
