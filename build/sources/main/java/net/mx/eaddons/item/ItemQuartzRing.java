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
import net.minecraft.entity.ai.attributes.IAttribute;
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
public class ItemQuartzRing extends Item implements IBauble {
    public static final ItemQuartzRing INSTANCE = new ItemQuartzRing();

    private static final UUID ARMOR_UUID = UUID.fromString("3b312dce-5f84-c7e5-fa4b-8021a74c3d96");
    private static final UUID LUCK_UUID = UUID.fromString("233c2c66-ef0c-4036-8101-6540abc9bf47");

    public static final double ARMOR_BONUS = 2.0;
    public static final double LUCK_BONUS = 1.5;
    public static final float MAGIC_RESISTANCE = 0.30F;

    public ItemQuartzRing() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("quartz_ring");
        setRegistryName("quartz_ring");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
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
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.quartz_ring.header"));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.quartz_ring.magic_res"));
            list.add(TextFormatting.BLUE + I18n.format("tooltip.eaddons.quartz_ring.armor"));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.quartz_ring.luck"));
        } else {
            list.add(I18n.format("tooltip.eaddons.quartz_ring.hold_shift"));
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
            removeModifier(player, SharedMonsterAttributes.LUCK, LUCK_UUID);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;
        ensureModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_UUID, "Quartz Ring Armor", ARMOR_BONUS);
        ensureModifier(player, SharedMonsterAttributes.LUCK, LUCK_UUID, "Quartz Ring Luck", LUCK_BONUS);
    }

    private static void ensureModifier(EntityPlayer player, IAttribute attr, UUID uuid, String name, double amount) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing == null) {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, 0));
        }
    }

    private static void removeModifier(EntityPlayer player, IAttribute attr, UUID uuid) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }

    public static boolean hasQuartzRing(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }
}
