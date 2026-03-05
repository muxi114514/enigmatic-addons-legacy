package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemEtheriumCore extends Item implements IBauble, keletu.enigmaticlegacy.util.interfaces.ISpellstone {

    public static final ItemEtheriumCore INSTANCE = new ItemEtheriumCore();

    private static final UUID ARMOR_UUID = UUID.fromString("554893CF-3922-46D9-A507-FAD7A7669EC0");
    private static final UUID ARMOR_MULT_UUID = UUID.fromString("B5118D05-AB68-49A1-A622-BFAF8340FAB5");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("DFEE887C-F2C0-491F-BCB6-4148855272CB");
    private static final UUID TOUGHNESS_MULT_UUID = UUID.fromString("A0A3F1D2-D324-4099-8B76-9351478CDD26");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("9B64D316-1C37-44F6-9E2F-239C8B1C0030");

    public ItemEtheriumCore() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("etherium_core");
        setRegistryName("etherium_core");
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
            int cooldownSec = EtheriumCoreConfig.cooldownTicks / 20;
            int shieldSec = EtheriumCoreConfig.shieldDurationTicks / 20;
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.etherium_core.active", shieldSec, cooldownSec));
            if (net.minecraft.client.Minecraft.getMinecraft().player != null && ItemForgerGem.hasLostEngine(net.minecraft.client.Minecraft.getMinecraft().player)) {
                int reducedSec = (int) (EtheriumCoreConfig.cooldownTicks * EtheriumCoreConfig.lostEngineCooldownFactor / 20);
                list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.etherium_core.cooldown_lost_engine", reducedSec));
            }
            int remaining = EtheriumCoreClientHelper.getCooldownRemainingSeconds();
            if (remaining > 0) {
                list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.etherium_core.cooldown_remaining", remaining));
            }
            list.add("");
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.etherium_core.passive_header"));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.etherium_core.armor", (int) EtheriumCoreConfig.armorBonus, (int) (EtheriumCoreConfig.armorMultiplier * 100)));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.etherium_core.toughness", (int) EtheriumCoreConfig.armorToughnessBonus, (int) (EtheriumCoreConfig.armorToughnessMultiplier * 100)));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.etherium_core.knockback", (int) (EtheriumCoreConfig.knockbackResistance * 100)));
            list.add(TextFormatting.YELLOW + I18n.format("tooltip.eaddons.etherium_core.damage_conversion", (int) (EtheriumCoreConfig.damageConversion * 100), EtheriumCoreConfig.damageConversionCap));
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.etherium_core.immunities"));
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.etherium_core.shield_threshold"));
        } else {
            list.add(I18n.format("tooltip.eaddons.etherium_core.hold_shift"));
        }
        list.add("");
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
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler((EntityPlayer) player);
        if (baubles == null) return true;
        for (int slot = 0; slot < baubles.getSlots(); slot++) {
            ItemStack equipped = baubles.getStackInSlot(slot);
            if (equipped.isEmpty()) continue;
            if (equipped.getItem() == INSTANCE) continue;
            if (isSpellstoneItem(equipped.getItem())) return false;
        }
        return true;
    }

    /** True if the item is an EnigmaticLegacy spellstone (same slot category as Golem Heart, Ocean Stone, etc.). */
    private static boolean isSpellstoneItem(net.minecraft.item.Item item) {
        try {
            Class<?> clazz = Class.forName("keletu.enigmaticlegacy.item.ItemSpellstoneBauble");
            return clazz.isInstance(item);
        } catch (Throwable ignored) {
            return false;
        }
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
            removeModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_MULT_UUID);
            removeModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID);
            removeModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_MULT_UUID);
            removeModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_UUID);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;
        ensureModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_UUID, "Etherium Core Armor", EtheriumCoreConfig.armorBonus, 0);
        ensureModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_MULT_UUID, "Etherium Core Armor %", EtheriumCoreConfig.armorMultiplier, 2);
        ensureModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID, "Etherium Core Toughness", EtheriumCoreConfig.armorToughnessBonus, 0);
        ensureModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_MULT_UUID, "Etherium Core Toughness %", EtheriumCoreConfig.armorToughnessMultiplier, 2);
        ensureModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_UUID, "Etherium Core KR", EtheriumCoreConfig.knockbackResistance, 0);
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

    public static boolean hasEtheriumCore(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }

    public static ItemStack getEtheriumCoreStack(EntityPlayer player) {
        int slot = BaublesApi.isBaubleEquipped(player, INSTANCE);
        if (slot >= 0) {
            ItemStack stack = BaublesApi.getBaubles(player).getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == INSTANCE) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Whether the player wears full EnigmaticLegacy Etherium armor (for shield threshold + client aura). */
    public static boolean hasFullEtheriumSet(EntityPlayer player) {
        if (player == null) return false;
        EntityEquipmentSlot[] slots = new EntityEquipmentSlot[]{
                EntityEquipmentSlot.HEAD,
                EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS,
                EntityEquipmentSlot.FEET
        };
        final String modId = "enigmaticlegacy";
        final String[] armorIds = {"etherium_helm", "etherium_chest", "etherium_legs", "etherium_boots"};
        for (EntityEquipmentSlot slot : slots) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) return false;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !modId.equals(id.getResourceDomain())) return false;
            String path = id.getResourcePath();
            boolean match = false;
            for (String armorId : armorIds) {
                if (armorId.equals(path)) { match = true; break; }
            }
            if (!match) return false;
        }
        return true;
    }

    @Override
    public void triggerActiveAbility(World world, EntityPlayerMP player, ItemStack stack) {
        // Etherium Core uses its own key + packet; no-op here so we are just treated as a spellstone for canEquip mutual exclusion.
    }
}
