package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
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

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemTotemOfMalice extends Item implements IBauble {
    public static final ItemTotemOfMalice INSTANCE = new ItemTotemOfMalice();
    private static final String TAG_DURABILITY = "TotemDurability";

    public ItemTotemOfMalice() {
        setMaxDamage(64);
        maxStackSize = 1;
        setUnlocalizedName("totem_of_malice");
        setRegistryName("totem_of_malice");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return TotemOfMaliceConfig.baseDurability
                + EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
    }

    /** Remaining uses (stored in NBT). New items default to max. */
    public static int getRemainingUses(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemTotemOfMalice))
            return 0;
        NBTTagCompound nbt = stack.getSubCompound("eaddons");
        if (nbt == null || !nbt.hasKey(TAG_DURABILITY)) {
            return INSTANCE.getMaxDamage(stack);
        }
        return nbt.getInteger(TAG_DURABILITY);
    }

    private static void setRemainingUses(ItemStack stack, int remaining) {
        int max = INSTANCE.getMaxDamage(stack);
        remaining = Math.max(0, Math.min(remaining, max));
        stack.getOrCreateSubCompound("eaddons").setInteger(TAG_DURABILITY, remaining);
    }

    @Override
    public int getDamage(ItemStack stack) {
        return getMaxDamage(stack) - getRemainingUses(stack);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        int max = getMaxDamage(stack);
        int remaining = Math.max(0, max - damage);
        setRemainingUses(stack, remaining);
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.MENDING)
            return false;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (repair.isEmpty()) return false;
        String configId = TotemOfMaliceConfig.repairItemRegistryName;
        if (configId == null || configId.isEmpty()) return false;
        ResourceLocation id;
        if (configId.indexOf(':') < 0) {
            id = new ResourceLocation("minecraft", configId);
        } else {
            String[] sp = configId.split(":", 2);
            id = new ResourceLocation(sp[0], sp[1]);
        }
        Item repairItem = ForgeRegistries.ITEMS.getValue(id);
        return repairItem != null && repair.getItem() == repairItem;
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
            list.add(TextFormatting.DARK_RED + I18n.format("tooltip.eaddons.totem_of_malice.header"));
            list.add(I18n.format("tooltip.eaddons.totem_of_malice.damage_illager", TotemOfMaliceConfig.damageBonusPercent + "%"));
            list.add(I18n.format("tooltip.eaddons.totem_of_malice.reduce_illager", TotemOfMaliceConfig.damageReductionPercent + "%"));
            list.add(I18n.format("tooltip.eaddons.totem_of_malice.cheat_death"));
            list.add(I18n.format("tooltip.eaddons.totem_of_malice.durability", getRemainingUses(stack), getMaxDamage(stack)));
        } else {
            list.add(I18n.format("tooltip.eaddons.totem_of_malice.hold_shift"));
        }
        list.add("");
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.AMULET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase entity) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
    }

    /** Whether the player has the totem in main hand, off hand, or bauble slot. */
    public static boolean hasTotemOfMalice(EntityPlayer player) {
        if (player == null) return false;
        if (!player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() == INSTANCE)
            return true;
        if (!player.getHeldItemOffhand().isEmpty() && player.getHeldItemOffhand().getItem() == INSTANCE)
            return true;
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }

    /** Returns the totem stack (main hand, then off hand, then bauble). Empty if none. */
    public static ItemStack getTotemStack(EntityPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack main = player.getHeldItemMainhand();
        if (!main.isEmpty() && main.getItem() == INSTANCE) return main;
        ItemStack off = player.getHeldItemOffhand();
        if (!off.isEmpty() && off.getItem() == INSTANCE) return off;
        int slot = BaublesApi.isBaubleEquipped(player, INSTANCE);
        if (slot != -1) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(slot);
            if (!bauble.isEmpty() && bauble.getItem() == INSTANCE) return bauble;
        }
        return ItemStack.EMPTY;
    }

    /** Count curse enchantments on the stack (each curse adds to death burst). */
    public static int getCurseCount(ItemStack stack) {
        int count = 0;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            if (e.getKey().isCurse())
                count += e.getValue();
        }
        return count;
    }

    /** Consume one use from the stack. Caller must ensure stack is the same reference as in hand/slot. */
    public static void consumeOneUse(ItemStack stack) {
        int remaining = getRemainingUses(stack) - 1;
        setRemainingUses(stack, remaining);
        if (remaining <= 0) {
            stack.setItemDamage(INSTANCE.getMaxDamage(stack));
        }
    }

    public static void register(net.minecraftforge.event.RegistryEvent.Register<Item> event) {
        event.getRegistry().register(INSTANCE);
    }

    public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
        net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(INSTANCE, 0,
                new net.minecraft.client.renderer.block.model.ModelResourceLocation("eaddons:totem_of_malice", "inventory"));
    }
}
