package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemEarthPromise extends Item implements IBauble {
    public static final ItemEarthPromise INSTANCE = new ItemEarthPromise();

    private static final UUID ARMOR_UUID = UUID.fromString("5b353dce-5f84-c0e8-fa7b-4821a77c3d82");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("4647d482-8f82-f4c8-5d72-e50a2989fc75");

    public ItemEarthPromise() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("earth_promise");
        setRegistryName("earth_promise");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.earth_promise.ability",
                    EarthPromiseConfig.abilityTriggerPercent + "%",
                    EarthPromiseConfig.cooldownTicks / 20));
            int remaining = EarthPromiseClientHelper.getCooldownRemainingSeconds();
            if (remaining > 0) {
                list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.earth_promise.cooldown_remaining", remaining));
            }
            list.add("");
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.earth_promise.ring_header"));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.earth_promise.armor", EarthPromiseConfig.armorBonus));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.earth_promise.toughness", EarthPromiseConfig.toughnessBonus));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.earth_promise.break_speed", EarthPromiseConfig.breakSpeedPercent + "%"));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.earth_promise.fortune", EarthPromiseConfig.fortuneBonus));
            EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
            if (player != null && ItemForgerGem.hasCursedRing(player)) {
                list.add("");
                list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.earth_promise.first_curse"));
                list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.earth_promise.first_curse_value", EarthPromiseConfig.firstCurseResistancePercent + "%"));
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.earth_promise.hold_shift"));
        }
        list.add("");
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            removeModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_UUID);
            removeModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;
        ensureModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_UUID, "Earth Promise Armor", EarthPromiseConfig.armorBonus);
        ensureModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID, "Earth Promise Toughness", EarthPromiseConfig.toughnessBonus);
    }

    private static void ensureModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid, String name, double amount) {
        net.minecraft.entity.ai.attributes.IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing == null) {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, 0));
        }
    }

    private static void removeModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid) {
        net.minecraft.entity.ai.attributes.IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }

    public static boolean hasEarthPromise(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }
}
