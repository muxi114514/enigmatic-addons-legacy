package net.mx.eaddons.item;

import java.util.List;

import javax.annotation.Nullable;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.entity.EntityItemIndestructible;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 极恶护甲：由以太套装用极恶锭升级而来，套装效果完全沿用以太护盾。
 * <p><b>刻意不继承 {@code EtheriumArmor}</b>——后者的构造器会 {@code EVENT_BUS.register(new EtheriumEventHandler())}，
 * 每造一件就多注册一个监听器；若继承，四件护甲会再叠四个监听器，护盾的伤害减免被重复结算。
 * 因此这里直接继承 {@link ItemArmor}，套装判定由 {@code MixinEtheriumArmor} 打通
 * （{@code EtheriumArmor.hasFullSet} 放行本护甲），从而复用 EL 原有的护盾、弹反、击退与音效。
 */
public class ItemEvilArmor extends ItemArmor {

    /** 耐久 165、护甲 5/8/10/5（合计 28，以太为 24）、附魔度 25、韧性 6.0（以太为 4.0）。 */
    public static final ArmorMaterial ARMOR_EVIL = EnumHelper.addArmorMaterial(
            "evil", "eaddons:evil", 165, new int[]{5, 8, 10, 5}, 25,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 6.0F);

    public static final ItemEvilArmor HELM = new ItemEvilArmor(EntityEquipmentSlot.HEAD, "evil_helm");
    public static final ItemEvilArmor CHEST = new ItemEvilArmor(EntityEquipmentSlot.CHEST, "evil_chest");
    public static final ItemEvilArmor LEGS = new ItemEvilArmor(EntityEquipmentSlot.LEGS, "evil_legs");
    public static final ItemEvilArmor BOOTS = new ItemEvilArmor(EntityEquipmentSlot.FEET, "evil_boots");

    public ItemEvilArmor(EntityEquipmentSlot slot, String name) {
        super(ARMOR_EVIL, slot == EntityEquipmentSlot.LEGS ? 2 : 1, slot);
        setMaxStackSize(1);
        setRegistryName(name);
        setUnlocalizedName(name);
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    /** 该物品是否属于极恶护甲（供套装判定使用）。 */
    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemEvilArmor;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        long time = entity != null && entity.world != null ? entity.world.getTotalWorldTime() : 0L;
        return ArmorTextureHelper.getTexture(ArmorTextureHelper.SET_EVIL, slot, time);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    /** 用极恶锭修复。 */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        Item evilIngot = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "evil_ingot"));
        return (evilIngot != null && repair.getItem() == evilIngot) || super.getIsRepairable(toRepair, repair);
    }

    // 掉落物不灭：沿用 EL 的不可摧毁掉落实体，与以太护甲一致
    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    @Nullable
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        EntityItemIndestructible item = new EntityItemIndestructible(world,
                location.posX, location.posY, location.posZ, stack);
        item.setDefaultPickupDelay();
        item.motionX = location.motionX;
        item.motionY = location.motionY;
        item.motionZ = location.motionZ;
        if (location instanceof EntityItem) {
            item.setThrower(((EntityItem) location).getThrower());
            item.setOwner(((EntityItem) location).getOwner());
        }
        return item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.evil_armor.desc1"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.evil_armor.desc2"));
            list.add("");
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.evil_armor.set_header"));
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.evil_armor.set1"));
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.evil_armor.set2"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.evil_armor.brief"));
            list.add(I18n.format("tooltip.eaddons.evil_armor.hold_shift"));
        }
        list.add("");
    }
}
