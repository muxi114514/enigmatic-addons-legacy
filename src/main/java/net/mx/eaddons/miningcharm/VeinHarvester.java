package net.mx.eaddons.miningcharm;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

/**
 * 连锁里每一块的挖掘。照原版 PlayerInteractionManager#tryHarvestBlock：先发 BreakEvent（保护、冒险模式逐块生效），
 * 再按手上工具判定可否采集、harvestBlock 掉落（时运 / 精准采集照算）、掉经验。
 * 去掉了两处工具回调：onBlockStartBreak（锤类工具会对每一块再做一次范围挖掘）与 onBlockDestroyed（扣耐久）。
 */
final class VeinHarvester {

    private VeinHarvester() {
    }

    static boolean isChainable(Block block) {
        return !(block instanceof BlockCommandBlock) && !(block instanceof BlockStructure);
    }

    static void harvest(EntityPlayerMP player, World world, BlockPos pos) {
        int exp = ForgeHooks.onBlockBreakEvent(world, player.interactionManager.getGameType(), player, pos);
        if (exp == -1) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        TileEntity tile = world.getTileEntity(pos);
        Block block = state.getBlock();
        // 原版的破坏粒子 / 音效不发给挖掘者本人（客户端自己预演了第一块），连锁的方块要连他一起发
        world.playEvent(null, 2001, pos, Block.getStateId(state));
        if (player.isCreative()) {
            removeBlock(player, world, pos, false);
            return;
        }
        boolean canHarvest = block.canHarvestBlock(world, pos, player);
        ItemStack tool = player.getHeldItemMainhand().copy();
        boolean removed = removeBlock(player, world, pos, canHarvest);
        if (removed && canHarvest) {
            block.harvestBlock(world, player, pos, state, tile, tool);
        }
        if (removed && exp > 0) {
            block.dropXpOnBlockBreak(world, pos, exp);
        }
    }

    private static boolean removeBlock(EntityPlayerMP player, World world, BlockPos pos, boolean canHarvest) {
        IBlockState state = world.getBlockState(pos);
        boolean removed = state.getBlock().removedByPlayer(state, world, pos, player, canHarvest);
        if (removed) {
            state.getBlock().onBlockDestroyedByPlayer(world, pos, state);
        }
        return removed;
    }
}
