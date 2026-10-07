package net.mx.eaddons.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** 术石残片：术质共鸣者的修复材料。 */
public class ItemSpellstoneDebris extends Item {
    public static final ItemSpellstoneDebris INSTANCE = new ItemSpellstoneDebris();

    public ItemSpellstoneDebris() {
        setUnlocalizedName("spellstone_debris");
        setRegistryName("spellstone_debris");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add("");
        list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_debris.brief"));
        list.add("");
    }
}
