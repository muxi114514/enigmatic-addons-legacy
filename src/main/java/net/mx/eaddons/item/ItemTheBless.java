package net.mx.eaddons.item;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.mx.eaddons.potion.PotionIchorCorrosion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemTheBless extends ItemSword {
    private static final ToolMaterial THE_BLESS_MATERIAL = EnumHelper.addToolMaterial(
            "THE_BLESS", 4, 2500, 10.0F, 6.0F, 22);

    public static final ItemTheBless INSTANCE = new ItemTheBless();

    public ItemTheBless() {
        super(THE_BLESS_MATERIAL);
        setUnlocalizedName("the_bless");
        setRegistryName("the_bless");
        setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 6.0D, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -1.6D, 0));
        }
        return map;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        super.hitEntity(stack, target, attacker);

        target.setFire(10);

        target.addPotionEffect(new PotionEffect(PotionIchorCorrosion.INSTANCE, 100, 0));

        List<PotionEffect> beneficialEffects = new ArrayList<>();
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (effect.getPotion().isBeneficial()) {
                beneficialEffects.add(effect);
            }
        }
        for (PotionEffect effect : beneficialEffects) {
            target.removePotionEffect(effect.getPotion());
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (GuiScreen.isShiftKeyDown()) {
            EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
            boolean hasCursedRing = false;
            if (player != null) {
                hasCursedRing = hasCursedRingEquipped(player);
            }

            if (hasCursedRing) {
                tooltip.add(I18n.format("tooltip.eaddons.the_bless.curse_fix"));
                tooltip.add("");
            }

            tooltip.add(I18n.format("tooltip.eaddons.the_bless.fire_bonus"));
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.debuff_remove"));
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.ichor"));
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.fire_immune"));
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.invuln"));
        } else {
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.desc1"));
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.desc2"));
            tooltip.add("");
            tooltip.add(I18n.format("tooltip.eaddons.the_bless.hold_shift"));
        }
    }

    private static boolean hasCursedRingEquipped(EntityPlayer player) {
        try {
            net.minecraft.item.Item cursedRing = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS
                    .getValue(new net.minecraft.util.ResourceLocation("enigmaticlegacy", "cursed_ring"));
            if (cursedRing == null) return false;
            return baubles.api.BaublesApi.isBaubleEquipped(player, cursedRing) != -1;
        } catch (Exception e) {
            return false;
        }
    }
}
