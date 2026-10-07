package net.mx.eaddons.elplus;

import java.util.List;

import javax.annotation.Nullable;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.entity.EntityItemIndestructible;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 行刑者之斧（EL+ 移植）：下界合金级的剑，面板 10 攻击、1.6 攻速、2031 耐久，
 * 击杀僵尸、骷髅、苦力怕、凋灵骷髅、末影龙时有概率斩下头颅（见 {@link ExecutionAxeHandler}）。
 * 掉落物不可摧毁，用下界合金锭（矿辞 ingotNetherite，含 EL 凋零合金锭）修复。
 */
public class ItemExecutionAxe extends ItemSword {

    /** ItemSword 攻击 = 3 + 材质伤害，面板再 +1，故材质伤害取 6 */
    private static final ToolMaterial MATERIAL = EnumHelper.addToolMaterial("EADDONS_EXECUTION_AXE", 4, 2031, 9.0F, 6.0F, 15);

    public static final ItemExecutionAxe INSTANCE = new ItemExecutionAxe();

    public ItemExecutionAxe() {
        super(MATERIAL);
        setUnlocalizedName("execution_axe");
        setRegistryName("execution_axe");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        for (ItemStack ore : OreDictionary.getOres("ingotNetherite", false)) {
            if (OreDictionary.itemMatches(ore, repair, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        EntityItemIndestructible item = new EntityItemIndestructible(world, location.posX, location.posY, location.posZ, stack);
        item.motionX = location.motionX;
        item.motionY = location.motionY;
        item.motionZ = location.motionZ;
        item.setDefaultPickupDelay();
        return item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.eaddons.executionAxe1"));
        tooltip.add(I18n.format("tooltip.eaddons.executionAxe2", percent(ElPlusConfig.beheadLootingBonus)));
        tooltip.add(I18n.format("tooltip.eaddons.executionAxe3"));
        tooltip.add("");
        tooltip.add(I18n.format("tooltip.eaddons.executionAxeBeheadingChance", percent(ElPlusConfig.beheadChance)));
    }

    private static String percent(float value) {
        return Math.round(value * 100) + "%";
    }
}
