package net.mx.eaddons.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemDragonBow extends ItemBow {
    public static final ItemDragonBow INSTANCE = new ItemDragonBow();

    public ItemDragonBow() {
        super();
        setMaxDamage(1024);
        setUnlocalizedName("dragon_bow");
        setRegistryName("dragon_bow");
        setMaxStackSize(1);
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    public static float getPowerForTime(int tick) {
        float f = (float) tick / 32.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    public static void resetEffect(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag("CustomPotionEffects");
        }
    }

    public static void addEffect(ItemStack stack, PotionEffect effect) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        @SuppressWarnings("null")
        NBTTagCompound tag = stack.getTagCompound();
        List<PotionEffect> effects = getCustomEffects(tag);
        int max = DragonBowConfig.maxPotionAmount;

        if (effects.size() < max) {
            effects.add(effect);
        } else {
            List<PotionEffect> newEffects = new ArrayList<>();
            for (int i = 1; i < effects.size(); i++) {
                newEffects.add(effects.get(i));
            }
            newEffects.add(effect);
            effects = newEffects;
        }

        if (!effects.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (PotionEffect e : effects) {
                list.appendTag(e.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            }
            tag.setTag("CustomPotionEffects", list);
        }
    }

    public static List<PotionEffect> getCustomEffects(NBTTagCompound tag) {
        List<PotionEffect> effects = new ArrayList<>();
        if (tag != null && tag.hasKey("CustomPotionEffects", 9)) {
            NBTTagList list = tag.getTagList("CustomPotionEffects", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int timeLeft) {
        if (!(user instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) user;

        int charge = this.getMaxItemUseDuration(stack) - timeLeft;
        float power = getPowerForTime(charge);
        if (power < 0.1F) return;

        stack.damageItem(1, player);

        if (!world.isRemote) {
            List<PotionEffect> effects = getCustomEffects(stack.getTagCompound());
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
            int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
            int multi = EnchantmentHelper.getEnchantmentLevel(EnchantmentMultishot.INSTANCE, stack) > 0 ? 1 : 0;

            for (int index = -multi; index < 1 + multi; index++) {
                EntityDragonBreathArrow arrow = new EntityDragonBreathArrow(world, player);
                arrow.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, power * 3.6F, 1.0F);
                arrow.motionX *= 0.95D;
                arrow.motionY = arrow.motionY * 0.95D + 0.135D * index;
                arrow.motionZ *= 0.95D;

                if (!effects.isEmpty()) {
                    for (PotionEffect effect : effects) {
                        arrow.addEffect(effect);
                    }
                }

                if (powerLevel > 0) {
                    arrow.setDamage(arrow.getDamage() + powerLevel * 0.8);
                }
                if (punch > 0) {
                    arrow.setKnockbackStrength(punch);
                }

                world.spawnEntity(arrow);
            }
        }

        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
                1.0F, 2.5F / (itemRand.nextFloat() * 0.4F + 1.2F) + power * 0.5F);

        player.addStat(StatList.getObjectUseStats(this));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        List<PotionEffect> effects = getCustomEffects(stack.getTagCompound());
        if (effects.isEmpty()) {
            tooltip.add(TextFormatting.DARK_PURPLE + "You can combine this with potions.");
        } else {
            for (PotionEffect effect : effects) {
                tooltip.add(TextFormatting.BLUE + effect.getEffectName());
            }
        }
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.INFINITY || enchantment == Enchantments.FLAME) {
            return false;
        }
        if (enchantment == EnchantmentMultishot.INSTANCE) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
