package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemForgerGem extends Item implements IBauble {
    public static final ItemForgerGem INSTANCE = new ItemForgerGem();

    public ItemForgerGem() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("forger_gem");
        setRegistryName("forger_gem");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.forger_gem.desc1"));
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.forger_gem.desc2"));

            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null && hasRingEquipped(player)) {
                list.add("");
                list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.forger_gem.unbreakable1"));
                list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.forger_gem.unbreakable2"));
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.forger_gem.hold_shift"));
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
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
    }

    public static boolean hasForgerGem(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }

    public static boolean hasCursedRing(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "cursed_ring"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    public static boolean hasBlessedRing(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "blessed_ring"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    /**
     * Equivalent to "isOKOne" in the high-version mod:
     * player has cursed_ring OR blessed_ring equipped.
     */
    public static boolean hasRingEquipped(EntityPlayer player) {
        return hasCursedRing(player) || hasBlessedRing(player);
    }

    /** Whether the player has Lost Engine (EnigmaticLegacy) equipped in Baubles. */
    public static boolean hasLostEngine(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "lost_engine"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }
}
