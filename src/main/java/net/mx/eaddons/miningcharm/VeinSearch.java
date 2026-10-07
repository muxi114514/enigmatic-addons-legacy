package net.mx.eaddons.miningcharm;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 从起点向外（含斜角的 26 格）找相连的同种方块，由近到远，不含起点。只读方块，不改世界。 */
final class VeinSearch {

    private VeinSearch() {
    }

    static List<BlockPos> find(World world, BlockPos start, IBlockState startState, EntityPlayer player, int max) {
        Kind kind = Kind.of(world, start, startState, player);
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = current.add(dx, dy, dz);
                        // isBlockLoaded 只查已加载区块，不会触发区块加载
                        if (!visited.add(next) || !world.isBlockLoaded(next)) {
                            continue;
                        }
                        IBlockState state = world.getBlockState(next);
                        if (!isMineable(world, next, state) || !kind.equals(Kind.of(world, next, state, player))) {
                            continue;
                        }
                        found.add(next);
                        if (found.size() >= max) {
                            return found;
                        }
                        queue.add(next);
                    }
                }
            }
        }
        return found;
    }

    static boolean isMineable(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        return !block.isAir(state, world, pos)
                && state.getBlockHardness(world, pos) >= 0
                && !MiningCharmConfig.veinBlacklist.contains(String.valueOf(block.getRegistryName()));
    }

    /**
     * 「同种」按选取方块（鼠标中键）得到的物品与 meta 判定：亮 / 暗红石矿、燃烧 / 熄灭的熔炉算一种，
     * 花岗岩与石头、不同木种的原木不算一种。取不到物品时退回方块 + 掉落 meta。
     */
    private static final class Kind {
        private final Object id;
        private final int meta;

        private Kind(Object id, int meta) {
            this.id = id;
            this.meta = meta;
        }

        static Kind of(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
            Block block = state.getBlock();
            ItemStack pick = ItemStack.EMPTY;
            try {
                RayTraceResult hit = new RayTraceResult(new Vec3d(pos).addVector(0.5, 0.5, 0.5), EnumFacing.UP, pos);
                pick = block.getPickBlock(state, hit, world, pos, player);
            } catch (RuntimeException ignored) {
                // 个别模组的选取逻辑依赖客户端状态，退回方块本身
            }
            if (pick == null || pick.isEmpty()) {
                return new Kind(block, block.damageDropped(state));
            }
            return new Kind(pick.getItem(), pick.getMetadata());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Kind)) {
                return false;
            }
            Kind other = (Kind) o;
            return this.id == other.id && this.meta == other.meta;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(this.id), this.meta);
        }
    }
}
