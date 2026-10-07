package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 混沌鞘翅（The Arrogance of Chaos）：移植自 1.20 神遗拓展 ChaosElytra。
 * 因 1.12.2 无 Curios 胸槽/Caelus，改做 Baubles 的 BODY（披风）饰品——不占胸甲。
 * 佩戴后可鞘翅滑翔、按跳跃加速冲刺、陡降时混沌俯冲 AOE，并获得定向减伤。
 * 仅「Worthy One」可佩戴。飞行/战斗逻辑见 {@link ChaosElytraEventHandler}。
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemChaosElytra extends Item implements IBauble {
    public static final ItemChaosElytra INSTANCE = new ItemChaosElytra();

    public ItemChaosElytra() {
        setMaxDamage(3600);
        maxStackSize = 1;
        setUnlocalizedName("chaos_elytra");
        setRegistryName("chaos_elytra");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
    }

    /** 仅「Worthy One」可佩戴（对应 1.20 的 isTheWorthyOne）。 */
    @Override
    @Optional.Method(modid = "baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase entity) {
        return entity instanceof EntityPlayer && SuperpositionHandler.isTheWorthyOne((EntityPlayer) entity);
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 逐帧逻辑在 ChaosElytraEventHandler 的 PlayerTickEvent 内统一处理
    }

    @Override
    @Optional.Method(modid = "baubles")
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.chaos_elytra.desc1",
                    ChaosElytraConfig.damageResistancePercent + "%"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.chaos_elytra.desc2"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.chaos_elytra.desc3"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.chaos_elytra.brief"));
            list.add(I18n.format("tooltip.eaddons.chaos_elytra.hold_shift"));
        }
        list.add("");
    }
}
