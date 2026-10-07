package net.mx.eaddons.elplus;

import java.util.Random;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCake;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * 永恒蛋糕（EL+ 移植）：6 口，每口按金胡萝卜回复；吃完停在最后一片，随机刻逐片恢复，等于无限食物。
 * 空手潜行右键一个完整的蛋糕可收回；挖掉、失去支撑、被活塞推、被炸都掉一个完整蛋糕（原版蛋糕都不掉，这里含寰宇之心不能丢）。
 */
public class BlockEternalCake extends BlockCake {

    public static final BlockEternalCake INSTANCE = new BlockEternalCake();
    public static final ItemBlock ITEM_BLOCK = createItem();

    /** 最多吃到的口数：BITES 到 6 就不能再吃 */
    private static final int MAX_EDIBLE_BITES = 5;

    public BlockEternalCake() {
        setHardness(0.5F);
        setSoundType(SoundType.CLOTH);
        setTickRandomly(true);
        disableStats();
        setUnlocalizedName("eternal_cake");
        setRegistryName("eternal_cake");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    private static ItemBlock createItem() {
        ItemBlock item = new ItemBlock(INSTANCE) {
            @Override
            public EnumRarity getRarity(ItemStack stack) {
                return EnumRarity.UNCOMMON;
            }
        };
        item.setMaxStackSize(1);
        item.setRegistryName("eternal_cake");
        return item;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        int bites = state.getValue(BITES);
        if (player.isSneaking() && bites == 0 && hand == EnumHand.MAIN_HAND && player.getHeldItemMainhand().isEmpty()) {
            if (!world.isRemote) {
                world.setBlockToAir(pos);
                if (!player.inventory.addItemStackToInventory(new ItemStack(ITEM_BLOCK))) {
                    player.dropItem(new ItemStack(ITEM_BLOCK), false);
                }
            }
            return true;
        }
        if (bites > MAX_EDIBLE_BITES || !player.canEat(false)) {
            return false;
        }
        if (!world.isRemote) {
            player.getFoodStats().addStats(6, 1.2F);
            world.setBlockState(pos, state.withProperty(BITES, bites + 1), 3);
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 0.5F,
                    world.rand.nextFloat() * 0.1F + 0.9F);
        }
        return true;
    }

    /** 随机刻恢复一片 */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(world, pos, state, rand);
        if (world.getBlockState(pos).getBlock() != this) {
            return;
        }
        int bites = state.getValue(BITES);
        if (bites > 0) {
            world.setBlockState(pos, state.withProperty(BITES, bites - 1), 3);
        }
    }

    /** 切片：给附属模组用（如神秘佳肴的以太弯刀），返回是否切下了一片 */
    public static boolean cutSlice(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != INSTANCE) {
            return false;
        }
        int bites = state.getValue(BITES);
        if (bites > MAX_EDIBLE_BITES) {
            return false;
        }
        world.setBlockState(pos, state.withProperty(BITES, bites + 1), 3);
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(ITEM_BLOCK);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(ITEM_BLOCK);
    }

    /** 挖掉、活塞推走都走这里；吃过几口都掉完整蛋糕，反正会自己长回来 */
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ITEM_BLOCK;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    /** 原版失去下方支撑时直接变空气，这里先掉落 */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.getBlockState(pos.down()).getMaterial().isSolid()) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    /** 原版爆炸按 1/威力 的概率掉落，这里改在 onBlockExploded 里必掉 */
    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
        super.onBlockExploded(world, pos, explosion);
    }
}
