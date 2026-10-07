package net.mx.eaddons.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 抓钩飞行一 tick 的碰撞检测：先射线找方块，再在截短后的路径上找最近的实体（实体优先）。
 * 写法照原版 {@code EntityFishHook}，1.12.2 没有 {@code Projectile} 基类的封装。
 */
final class EngineHookCollision {

    private EngineHookCollision() {
    }

    /** @return 方块或实体命中；都没碰到返回 null */
    @Nullable
    static RayTraceResult trace(Entity hook, @Nullable Entity owner) {
        Vec3d start = new Vec3d(hook.posX, hook.posY, hook.posZ);
        Vec3d end = new Vec3d(hook.posX + hook.motionX, hook.posY + hook.motionY, hook.posZ + hook.motionZ);
        RayTraceResult result = hook.world.rayTraceBlocks(start, end, false, true, false);
        if (result != null) {
            end = new Vec3d(result.hitVec.x, result.hitVec.y, result.hitVec.z);
        }
        Entity target = findEntityOnPath(hook, owner, start, end);
        return target != null ? new RayTraceResult(target) : result;
    }

    @Nullable
    private static Entity findEntityOnPath(Entity hook, @Nullable Entity owner, Vec3d start, Vec3d end) {
        Entity found = null;
        double closest = 0.0D;
        AxisAlignedBB box = hook.getEntityBoundingBox()
                .expand(hook.motionX, hook.motionY, hook.motionZ).grow(1.0D);
        List<Entity> list = hook.world.getEntitiesWithinAABBExcludingEntity(hook, box);
        for (Entity entity : list) {
            if (entity == owner || (!entity.canBeCollidedWith() && !(entity instanceof EntityItem))) {
                continue;
            }
            AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(0.3D);
            RayTraceResult hit = aabb.calculateIntercept(start, end);
            if (hit == null) {
                continue;
            }
            double dist = start.squareDistanceTo(hit.hitVec);
            if (dist < closest || closest == 0.0D) {
                found = entity;
                closest = dist;
            }
        }
        return found;
    }
}
