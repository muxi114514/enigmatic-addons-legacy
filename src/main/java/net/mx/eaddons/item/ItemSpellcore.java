package net.mx.eaddons.item;

import net.minecraft.client.gui.GuiScreen;
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

/**
 * 术质核心：共鸣者吃入术石后在副手留下的凭证。
 * 两个用途——放回副手长按右键可解除共鸣取回术石；潜行右键则消耗一枚提升共鸣等级。
 */
public class ItemSpellcore extends Item {
    public static final ItemSpellcore INSTANCE = new ItemSpellcore();

    public ItemSpellcore() {
        setUnlocalizedName("spellcore");
        setRegistryName("spellcore");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(16);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.spellcore.desc1"));
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.spellcore.desc2"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellcore.brief"));
            list.add(I18n.format("tooltip.eaddons.spellcore.hold_shift"));
        }
        list.add("");
    }
}
