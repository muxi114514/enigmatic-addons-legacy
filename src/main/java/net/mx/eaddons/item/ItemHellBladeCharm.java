package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemHellBladeCharm extends Item implements IBauble {
    public static final ItemHellBladeCharm INSTANCE = new ItemHellBladeCharm();

    private static final UUID ARMOR_UUID = UUID.fromString("E7EC5AC7-8D8C-9D83-A87C-1830B55951FA");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("03153759-3B92-E47E-EFED-DD4F2ECA6B47");
    private static final String TAG_COOLDOWN = "HellBladeCooldownEnd";

    public static final int EQUIP_COOLDOWN = 1200;
    public static final float DAMAGE_MULTIPLIER = 1.0F;
    public static final float ARMOR_DEBUFF = 1.0F;
    public static final float KILL_THRESHOLD = 0.75F;
    public static final float KILL_CURSED_THRESHOLD = 0.50F;
    public static final float HEAL_MULTIPLIER = 0.8F;
    public static final float DAMAGE_TAKEN_BONUS = 0.1F;

    public ItemHellBladeCharm() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("hell_blade_charm");
        setRegistryName("hell_blade_charm");
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
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        ensureCurseEnchantments(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");

        if (GuiScreen.isShiftKeyDown()) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            boolean hasBerserk = player != null && hasBerserkEmblem(player);
            boolean hasCursed = player != null && hasCursedRing(player);
            boolean hasScroll = player != null && hasCursedScroll(player);

            int armorPct = hasBerserk ? 60 : 100;
            int dmgBoostPct = hasBerserk ? 125 : 100;
            int dmgTakenPct = hasBerserk ? 5 : 10;
            int thresholdPct = hasCursed ? 50 : 75;
            int cooldownSec = hasCursed ? 120 : 60;

            list.add(I18n.format("tooltip.eaddons.hell_blade.header"));
            list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.hell_blade.armor", armorPct));
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.hell_blade.damage", dmgBoostPct));
            list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.hell_blade.damage_taken", dmgTakenPct));
            list.add("");
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.hell_blade.execute", thresholdPct));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.hell_blade.heal"));
            list.add("");
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.hell_blade.cooldown", cooldownSec));

            if (hasBerserk || hasCursed || hasScroll) {
                list.add("");
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.hell_blade.synergy"));
            }
            if (hasCursed) {
                list.add(TextFormatting.DARK_RED + I18n.format("tooltip.eaddons.hell_blade.cursed_ring1"));
                list.add(TextFormatting.DARK_RED + I18n.format("tooltip.eaddons.hell_blade.cursed_ring2"));
            }
            if (hasScroll) {
                list.add(TextFormatting.DARK_AQUA + I18n.format("tooltip.eaddons.hell_blade.cursed_scroll"));
            }
            if (hasBerserk) {
                list.add(TextFormatting.YELLOW + I18n.format("tooltip.eaddons.hell_blade.berserk1"));
                list.add(TextFormatting.YELLOW + I18n.format("tooltip.eaddons.hell_blade.berserk2"));
                list.add(TextFormatting.YELLOW + I18n.format("tooltip.eaddons.hell_blade.berserk3"));
            }

            if (player != null && hasHellBladeCharm(player)) {
                int remaining = getCooldownRemaining(stack, player.world.getTotalWorldTime());
                if (remaining > 0) {
                    list.add("");
                    list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.hell_blade.locked", remaining / 20));
                }
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.hell_blade.hold_shift"));
        }

        list.add("");
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.CHARM;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer) || player.world.isRemote)
            return;
        EntityPlayer ep = (EntityPlayer) player;
        if (ep.isCreative() || ep.isSpectator())
            return;

        int cooldown = EQUIP_COOLDOWN;
        if (hasCursedRing(ep))
            cooldown *= 2;
        if (ItemForgerGem.hasLostEngine(ep))
            cooldown = (int) (cooldown * EarthPromiseConfig.lostEngineCooldownFactor);

        NBTTagCompound nbt = itemstack.getOrCreateSubCompound("eaddons");
        nbt.setLong(TAG_COOLDOWN, player.world.getTotalWorldTime() + cooldown);
    }

    @Override
    @Optional.Method(modid = "baubles")
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) player;
            if (ep.isCreative() || ep.isSpectator())
                return true;
        }
        NBTTagCompound nbt = itemstack.getSubCompound("eaddons");
        if (nbt != null && nbt.hasKey(TAG_COOLDOWN)) {
            return player.world.getTotalWorldTime() >= nbt.getLong(TAG_COOLDOWN);
        }
        return true;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote)
            return;
        EntityPlayer player = (EntityPlayer) entity;
        ensureCurseEnchantments(itemstack);
        double armorReduction = hasBerserkEmblem(player) ? 0.6 : ARMOR_DEBUFF;
        ensureModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_UUID, "Hell Blade Armor", -armorReduction);
        ensureModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID, "Hell Blade Toughness", -armorReduction);
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

    private static void ensureModifier(EntityPlayer player, IAttribute attr, UUID uuid, String name, double amount) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) > 0.001) {
                inst.removeModifier(uuid);
                inst.applyModifier(new AttributeModifier(uuid, name, amount, 2));
            }
        } else {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, 2));
        }
    }

    private static void removeModifier(EntityPlayer player, IAttribute attr, UUID uuid) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }

    /**
     * Embeds Curse of Vanishing + Curse of Binding into the ItemStack.
     * SuperpositionHandler.getCurseAmount iterates getFullEquipment (which includes baubles)
     * and counts enchantments where isCurse()==true. These 2 curses make the charm
     * contribute +2 to the curse count, matching the high-version Mixin behavior.
     * HideFlags=1 suppresses the enchantment lines in the tooltip.
     */
    private static void ensureCurseEnchantments(ItemStack stack) {
        Map<net.minecraft.enchantment.Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        boolean changed = false;
        if (!enchants.containsKey(Enchantments.VANISHING_CURSE)) {
            enchants.put(Enchantments.VANISHING_CURSE, 1);
            changed = true;
        }
        if (!enchants.containsKey(Enchantments.BINDING_CURSE)) {
            enchants.put(Enchantments.BINDING_CURSE, 1);
            changed = true;
        }
        if (changed) {
            EnchantmentHelper.setEnchantments(enchants, stack);
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.getInteger("HideFlags") != 1) {
            tag.setInteger("HideFlags", 1);
        }
    }

    // --- Bauble detection helpers ---

    public static boolean hasHellBladeCharm(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }

    public static boolean hasCursedRing(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "cursed_ring"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    public static boolean hasCursedScroll(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "cursed_scroll"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    public static boolean hasBerserkEmblem(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "berserk_emblem"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    public static float getKillThreshold(EntityPlayer player) {
        return hasCursedRing(player) ? KILL_CURSED_THRESHOLD : KILL_THRESHOLD;
    }

    public static float getDamageMultiplier(EntityPlayer player) {
        float mult = DAMAGE_MULTIPLIER;
        if (hasBerserkEmblem(player))
            mult *= 1.25F;
        return mult;
    }

    public static float getMissingHealthPool(EntityPlayer player) {
        return (player.getMaxHealth() - Math.min(player.getHealth(), player.getMaxHealth())) / player.getMaxHealth();
    }

    public static int getCooldownRemaining(ItemStack stack, long worldTime) {
        NBTTagCompound nbt = stack.getSubCompound("eaddons");
        if (nbt != null && nbt.hasKey(TAG_COOLDOWN)) {
            long end = nbt.getLong(TAG_COOLDOWN);
            return (int) Math.max(0, end - worldTime);
        }
        return 0;
    }

    // --- Registration ---

    public static void register(net.minecraftforge.event.RegistryEvent.Register<Item> event) {
        event.getRegistry().register(INSTANCE);
    }

    public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
        net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(INSTANCE, 0,
                new net.minecraft.client.renderer.block.model.ModelResourceLocation("eaddons:hell_blade_charm", "inventory"));
    }
}
