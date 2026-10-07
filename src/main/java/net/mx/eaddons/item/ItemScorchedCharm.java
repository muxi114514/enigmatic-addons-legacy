package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 焦阳护符：移植自 1.20 神遗拓展 ScorchedCharm。
 * 一枚火系饰品：免疫火焰伤害、岩浆行走并回血、攻击着火目标吸血、几率抗伤。
 * 仅"受祝福者/受诅咒者"可佩戴。免疫/吸血/抗伤见 {@link ScorchedCharmEventHandler}。
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemScorchedCharm extends Item implements IBauble {
    public static final ItemScorchedCharm INSTANCE = new ItemScorchedCharm();
    /** 陷进岩浆时往上托的速度（格/tick） */
    private static final double LAVA_RISE_SPEED = 0.15;
    /** 脚离岩浆表面不到这个高度就算落在表面上 */
    private static final double SURFACE_SNAP = 0.1;

    public ItemScorchedCharm() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("scorched_charm");
        setRegistryName("scorched_charm");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.CHARM;
    }

    /**
     * 是否可佩戴：不再硬编码"受诅咒者专属"，改为遵循 EL 的受诅咒/受祝福物品配置
     * （ItemBeCursed / ItemBeBlessed）。默认已由本模组将 eaddons:scorched_charm 注册进两张表，
     * 使受诅咒者或受祝福者可用；若从配置移除该项，则任何玩家均可佩戴。
     */
    @Override
    @Optional.Method(modid = "baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase entity) {
        return CursedEquipHelper.canEquip(entity, itemstack);
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
        // 永不燃烧
        if (entity.isBurning()) {
            entity.extinguish();
        }

        // SimpleDifficulty 控温：把温度压在过热线以下并清除过热效果（软依赖，装了才生效）
        if (net.mx.eaddons.compat.ModCompat.SIMPLE_DIFFICULTY
                && !entity.world.isRemote && entity instanceof EntityPlayer) {
            net.mx.eaddons.compat.SimpleDifficultyCompat.controlHeat(
                    (EntityPlayer) entity, ScorchedCharmConfig.sdMaxTemperatureLevel);
        }

        if (!entity.isSneaking()) {
            walkOnLava(entity);
        }

        // 岩浆中回血（站在岩浆表面也算）
        if (isInOrOnLava(entity) && entity.ticksExisted % 20 == 0) {
            entity.heal((float) ScorchedCharmConfig.lavaHealAmount);
        }
    }

    /**
     * 岩浆行走，写法同 Cyclic 水上行走：脚下是岩浆、脚所在格可通行、正在下落时，落到岩浆表面就清零竖直速度并视为站在地面，
     * 人始终在岩浆上方，不受岩浆减速；脚陷进岩浆里就往上托，直到脚离开岩浆。潜行时不调用，可以下沉。
     * <p>Baubles 在玩家 tick 末尾调用：这里改的速度作用于下一 tick 的移动，onGround 让下一 tick 能起跳、按地面摩擦行走。
     * 只读玩家脚下两格，所在区块必然已加载。
     */
    private static void walkOnLava(EntityLivingBase entity) {
        World world = entity.world;
        BlockPos feet = new BlockPos(entity.posX, entity.posY, entity.posZ);
        if (isLava(world, feet)) {
            if (entity.motionY < LAVA_RISE_SPEED) entity.motionY = LAVA_RISE_SPEED;
            entity.fallDistance = 0;
            return;
        }
        if (entity.motionY >= 0 || !isLavaSurface(world, feet)) {
            return;
        }
        double gap = entity.posY - feet.getY();
        if (gap < SURFACE_SNAP) {
            entity.motionY = 0;
            entity.onGround = true;
            // 原先掉进岩浆不摔伤，落在表面上也不摔伤
            entity.fallDistance = 0;
        } else if (entity.motionY < -gap) {
            // 下一 tick 会穿过表面：只落到表面为止
            entity.motionY = -gap;
        }
    }

    /** 回血与抗伤翻倍所说的「处于岩浆中」：陷在岩浆里，或站在岩浆表面（离表面不到 SURFACE_SNAP）。 */
    public static boolean isInOrOnLava(EntityLivingBase entity) {
        if (entity.isInLava()) return true;
        BlockPos feet = new BlockPos(entity.posX, entity.posY, entity.posZ);
        return entity.posY - feet.getY() < SURFACE_SNAP && isLavaSurface(entity.world, feet);
    }

    /** 脚所在格可通行、正下方是岩浆。 */
    private static boolean isLavaSurface(World world, BlockPos feet) {
        return isLava(world, feet.down()) && !world.getBlockState(feet).getMaterial().blocksMovement();
    }

    private static boolean isLava(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.LAVA;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.scorched_charm.desc1"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.scorched_charm.desc2"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.scorched_charm.desc3"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.scorched_charm.desc4"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.scorched_charm.brief"));
            list.add(I18n.format("tooltip.eaddons.scorched_charm.hold_shift"));
        }
        // "仅受诅咒者可用"的提示由 EL 依据其配置自动追加，这里不再重复
        list.add("");
    }

    public static boolean isWorn(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }

    /** 免疫的火焰伤害类型（1.12.2 字符串）。 */
    public static boolean isFireImmuneDamage(String damageType) {
        return "inFire".equals(damageType) || "onFire".equals(damageType)
                || "hotFloor".equals(damageType) || "lava".equals(damageType);
    }
}
