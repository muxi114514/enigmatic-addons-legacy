package net.mx.eaddons.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 抓钩绳索的绕障分段。
 *
 * <p>没有它的话绳子是一条直线，绕墙角时会从方块里穿过去，玩家也会被径直拽进墙里。
 * 这里维护一串转折点：绳子从钩子出发，依次经过这些点，最后连到玩家。
 * 牵引玩家时用的是**离玩家最近的那个转折点**，而不是钩子本体，于是拐角处会自然地把人先拉到角外。
 *
 * <p>思路上参考了整合包里 grapplemod 的绳段做法（撞角就分段、视线通畅就合段），实现是自己写的。
 *
 * <p>服务端维护一份并同步给客户端，供其他玩家渲染；持有者本人的客户端另算一份（{@code EngineRopeController}），
 * 摆荡过角时不必等同步延迟。
 */
public final class HookRope {

    /** 转折点上限，防止在复杂地形里无限加点。 */
    private static final int MAX_POINTS = 8;
    /** 绕过拐角时把转折点往方块外侧推开的距离。 */
    private static final double CORNER_OFFSET = 0.25D;

    /** 从钩子端往玩家端排列的转折点，空表示绳子可以直连。 */
    private final List<Vec3d> points = new ArrayList<>();

    /** 离玩家最近的那个点；没有转折点时就是钩子自己的位置。 */
    public Vec3d anchorFor(Vec3d hookPos) {
        return this.points.isEmpty() ? hookPos : this.points.get(this.points.size() - 1);
    }

    /** 钩子沿各转折点走到锚点的绳长；没有转折点时为 0。 */
    public double lengthToAnchor(Vec3d hookPos) {
        double length = 0.0D;
        Vec3d prev = hookPos;
        for (Vec3d point : this.points) {
            length += prev.distanceTo(point);
            prev = point;
        }
        return length;
    }

    public boolean isEmpty() {
        return this.points.isEmpty();
    }

    public List<Vec3d> getPoints() {
        return this.points;
    }

    public void clear() {
        this.points.clear();
    }

    /**
     * 每 tick 更新一次绳段。
     *
     * @return 转折点是否发生了变化（变了才需要重新同步给客户端）
     */
    public boolean update(World world, Vec3d hookPos, Vec3d playerEye) {
        // rayTraceBlocks 内部走 World#getBlockState，碰到未加载区块会触发同步加载，
        // 按项目规范先确认两端区块都在内存里；抓钩最远 64 格，正常情况都在视距内
        if (!isLoaded(world, hookPos) || !isLoaded(world, playerEye)) {
            return false;
        }
        boolean changed = tryRelease(world, hookPos, playerEye);
        changed |= tryAdd(world, hookPos, playerEye);
        return changed;
    }

    private static boolean isLoaded(World world, Vec3d pos) {
        return world.getChunkProvider().getLoadedChunk(
                net.minecraft.util.math.MathHelper.floor(pos.x) >> 4,
                net.minecraft.util.math.MathHelper.floor(pos.z) >> 4) != null;
    }

    /** 玩家与最近转折点之间被挡住 → 在拐角处插入一个新的转折点。 */
    private boolean tryAdd(World world, Vec3d hookPos, Vec3d playerEye) {
        if (this.points.size() >= MAX_POINTS) {
            return false;
        }
        Vec3d anchor = anchorFor(hookPos);
        RayTraceResult hit = world.rayTraceBlocks(playerEye, anchor, false, true, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }
        Vec3d corner = cornerOf(hit, anchor);
        // 新点和旧点挨得太近就不加，否则拐角处会疯狂堆点
        if (corner.squareDistanceTo(anchor) < 0.04D) {
            return false;
        }
        this.points.add(corner);
        return true;
    }

    /** 玩家与倒数第二个点（或钩子）之间已经通畅 → 弹掉最后一个转折点。 */
    private boolean tryRelease(World world, Vec3d hookPos, Vec3d playerEye) {
        if (this.points.isEmpty()) {
            return false;
        }
        Vec3d previous = this.points.size() == 1 ? hookPos : this.points.get(this.points.size() - 2);
        RayTraceResult hit = world.rayTraceBlocks(playerEye, previous, false, true, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return false;
        }
        this.points.remove(this.points.size() - 1);
        return true;
    }

    /**
     * 把射线命中点挪到方块外侧，作为转折点。
     * <p>先沿命中面的法线推开，再朝目标方向推一点，让绳子贴着角的外缘而不是嵌在面里。
     */
    private static Vec3d cornerOf(RayTraceResult hit, Vec3d towards) {
        Vec3d point = hit.hitVec;
        EnumFacing side = hit.sideHit;
        // 1.12.2 的 EnumFacing 还没有 getXOffset 那组方法，走 getDirectionVec
        net.minecraft.util.math.Vec3i dir = side.getDirectionVec();
        Vec3d normal = new Vec3d(dir.getX(), dir.getY(), dir.getZ());
        Vec3d along = towards.subtract(point);
        double len = along.lengthVector();
        if (len > 1.0E-4D) {
            along = along.scale(1.0D / len);
        }
        return point.add(normal.scale(CORNER_OFFSET)).add(along.scale(CORNER_OFFSET));
    }

    // ============================ 同步 ============================

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Vec3d point : this.points) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setDouble("x", point.x);
            entry.setDouble("y", point.y);
            entry.setDouble("z", point.z);
            list.appendTag(entry);
        }
        tag.setTag("Rope", list);
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.points.clear();
        NBTTagList list = tag.getTagList("Rope", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            this.points.add(new Vec3d(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z")));
        }
    }
}
