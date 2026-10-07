package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.item.EAddonsSpellstoneBauble;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 幻影灵魂灯笼：移植自 1.20 神遗拓展 IllusionLantern（完整移植）。
 * 一枚防御/吸血向咒石饰品：生成灵魂火球自动攻击、伤害分摊给周围敌人、大幅减免穿甲伤害、
 * 普通不死不主动攻击；代价是非魔法伤害易伤、日照受诅咒伤害、周围植物枯萎。
 * 伤害相关逻辑见 {@link IllusionLanternEventHandler}，被动光环在本类 onWornTick。
 */
public class ItemIllusionLantern extends EAddonsSpellstoneBauble {
    public static final ItemIllusionLantern INSTANCE = new ItemIllusionLantern();

    /** 日照诅咒伤害源：绕过护甲且为绝对伤害，避免被灯笼自身的减伤/易伤再次修饰。 */
    public static final DamageSource EVIL_CURSE =
            new DamageSource("evil_curse").setDamageBypassesArmor().setDamageIsAbsolute();

    private static final int MAX_BALLS = 5;

    public ItemIllusionLantern() {
        super("illusion_lantern", EnumRarity.RARE);
    }

    // ============================ 被动光环 ============================

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        super.onWornTick(stack, entity);
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        World world = player.world;

        // 1. 生成灵魂火球（上限 5 个，冷却由物品冷却追踪器管理）
        if (!player.getCooldownTracker().hasCooldown(this)) {
            List<EntitySoulFlameBall> balls = world.getEntitiesWithinAABB(
                    EntitySoulFlameBall.class, player.getEntityBoundingBox().grow(3.0));
            if (balls.size() < MAX_BALLS) {
                player.getCooldownTracker().setCooldown(this, IllusionLanternConfig.fireballCooldown);
                world.spawnEntity(new EntitySoulFlameBall(world, player, balls.size()));
            }
        }

        if (player.ticksExisted % 20 != 0 || player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }

        BlockPos pos = player.getPosition();

        // 2. 日晒诅咒：白天露天暴晒时受伤（夜晚/遮蔽安全）。
        // 用「存储天光 - 当前天光削减」得到实时天光：晴日正午露天 15-0=15，夜晚 15-11≈4，
        // 阴雨会加大削减，从而白天灼伤、夜晚安全，且雨天有一定遮蔽（同原版亡灵日晒逻辑）。
        int actualSkyLight = world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos)
                - world.getSkylightSubtracted();
        if (world.canSeeSky(pos) && actualSkyLight > 9) {
            // 头部有防具则由头盔挡下并缓慢损耗其耐久，否则本体受诅咒伤害
            ItemStack helmet = player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD);
            boolean protectedByHelmet = !helmet.isEmpty()
                    && (helmet.getItem() instanceof net.minecraft.item.ItemArmor || helmet.isItemStackDamageable());
            if (protectedByHelmet) {
                if (helmet.isItemStackDamageable()) {
                    helmet.damageItem(1, player);
                }
            } else {
                player.attackEntityFrom(EVIL_CURSE, player.getMaxHealth() / 5.0F);
            }
        }

        // 3. 枯萎：摧毁周围 5 格内的植物
        for (BlockPos p : BlockPos.getAllInBoxMutable(pos.add(-5, -5, -5), pos.add(5, 5, 5))) {
            if (!world.isBlockLoaded(p)) continue;
            if (isVegetation(world.getBlockState(p))) {
                world.destroyBlock(p.toImmutable(), false);
            }
        }
    }

    /** 广义植物判定，近似 1.20 的 FLOWERS/SAPLINGS/REPLACEABLE/REPLACEABLE_BY_TREES/SWORD_EFFICIENT。 */
    private static boolean isVegetation(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockFlower || block instanceof BlockSapling
                || block instanceof BlockDoublePlant || block instanceof BlockTallGrass
                || block instanceof BlockBush || block instanceof BlockCrops
                || block instanceof BlockStem || block instanceof BlockLeaves
                || block instanceof BlockVine || block instanceof BlockCactus) {
            return true;
        }
        Material m = state.getMaterial();
        return m == Material.PLANTS || m == Material.VINE || m == Material.LEAVES
                || m == Material.CACTUS || m == Material.GOURD;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.illusion_lantern.desc1"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.illusion_lantern.desc2"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.illusion_lantern.desc3"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.illusion_lantern.desc4"));
            list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.illusion_lantern.desc5"));
            list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.illusion_lantern.desc6"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.illusion_lantern.brief"));
            list.add(I18n.format("tooltip.eaddons.illusion_lantern.hold_shift"));
        }
        list.add("");
    }

    public static boolean isWorn(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, INSTANCE) != -1;
    }
}
