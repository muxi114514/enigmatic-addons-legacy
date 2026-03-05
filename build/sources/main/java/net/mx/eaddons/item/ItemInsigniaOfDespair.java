package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
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
public class ItemInsigniaOfDespair extends Item implements IBauble {

    public static final ItemInsigniaOfDespair INSTANCE = new ItemInsigniaOfDespair();
    private static final UUID DAMAGE_UUID = UUID.fromString("6776b12a-e8b9-4053-a7d1-8d3712443318");
    private static final UUID SPEED_UUID = UUID.fromString("881fd974-05bf-494c-ba42-c9718e275a01");
    private static final UUID MOVE_UUID = UUID.fromString("d3f24c58-05f3-4fa3-81c7-f90ff5ef98bc");
    private static final UUID KB_UUID = UUID.fromString("28532b8c-20d0-43fc-846c-1c60c074824c");

    public ItemInsigniaOfDespair() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("insignia_of_despair");
        setRegistryName("insignia_of_despair");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.eaddons.insignia_despair.line1"));
        list.add(TextFormatting.DARK_GRAY + net.minecraft.client.resources.I18n.format("tooltip.eaddons.insignia_despair.line2"));
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.TRINKET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return true;
        return BaublesApi.isBaubleEquipped((EntityPlayer) player, ItemEmblemOfAdventurer.INSTANCE) == -1;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            removeModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE_UUID);
            removeModifier(player, SharedMonsterAttributes.ATTACK_SPEED, SPEED_UUID);
            removeModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, MOVE_UUID);
            removeModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KB_UUID);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;
        double damage = EmblemAdventurerConfig.despairAttackDamage;
        double speedMult = EmblemAdventurerConfig.despairAttackSpeedPercent / 100.0;
        double moveMult = EmblemAdventurerConfig.despairMovementSpeedPercent / 100.0;
        double kbMult = EmblemAdventurerConfig.despairKnockbackResistancePercent / 100.0;
        ensureModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE_UUID, "Insignia Despair Damage", damage, 0);
        ensureModifier(player, SharedMonsterAttributes.ATTACK_SPEED, SPEED_UUID, "Insignia Despair Speed", speedMult, 2);
        ensureModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, MOVE_UUID, "Insignia Despair Move", moveMult, 2);
        ensureModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KB_UUID, "Insignia Despair KB", kbMult, 2);
    }

    private static void ensureModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid, String name, double amount, int operation) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing == null) {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }

    private static void removeModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }
}
