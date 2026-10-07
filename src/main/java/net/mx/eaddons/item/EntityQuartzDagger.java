package net.mx.eaddons.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * 魔法石英权杖发射的投射物，移植自 1.20 神遗拓展 ThrownQuartzDagger。
 * 机制：END_ROD 粒子拖尾、吸附附近掉落物到主人身边、命中清空目标无敌帧（可连击）、
 * 落地或超时后爆散消失、禁止拾取。基于 1.12.2 的 {@link EntityArrow} 实现。
 */
public class EntityQuartzDagger extends EntityArrow {
    /** 落地后的存活计数，用于延时爆散。 */
    private int inGroundTick;

    public EntityQuartzDagger(World world) {
        super(world);
        this.pickupStatus = PickupStatus.DISALLOWED;
    }

    public EntityQuartzDagger(World world, EntityLivingBase shooter) {
        super(world, shooter);
        this.pickupStatus = PickupStatus.DISALLOWED;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.world.isRemote) {
            // 客户端：沿飞行方向拖出 END_ROD 粒子
            Vec3d movement = new Vec3d(this.motionX, this.motionY, this.motionZ).scale(0.2);
            this.world.spawnParticle(EnumParticleTypes.END_ROD,
                    this.posX, this.posY, this.posZ,
                    movement.x, movement.y, movement.z);
        } else {
            // 服务端：把附近掉落物吸到主人眼前，对应原版"匕首帮忙捡起掉落物"
            if (this.shootingEntity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) this.shootingEntity;
                List<EntityItem> items = this.world.getEntitiesWithinAABB(
                        EntityItem.class, this.getEntityBoundingBox().grow(3.0));
                if (!items.isEmpty()) {
                    Vec3d eye = player.getPositionEyes(1.0F);
                    for (EntityItem item : items) {
                        item.setPosition(eye.x, eye.y, eye.z);
                    }
                }
            }
        }

        if (this.inGround) {
            this.inGroundTick++;
        }
        // 飞行 25 tick 后恢复重力，模拟原版逐渐下坠
        if (this.ticksExisted > 25 && this.hasNoGravity()) {
            this.setNoGravity(false);
        }

        if (this.inGroundTick == 2 || this.ticksExisted == 40) {
            spawnBurstParticles();
        } else if (this.inGroundTick > 2 || this.ticksExisted > 40) {
            this.setDead();
        }
    }

    @Override
    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit != null) {
            // 清空目标无敌帧，使多把匕首都能连续造成伤害
            result.entityHit.hurtResistantTime = 0;
            spawnBurstParticles();
        }
    }

    /** 客户端向四周发散一圈 END_ROD 粒子。 */
    private void spawnBurstParticles() {
        if (!this.world.isRemote) return;
        for (int i = 0; i < 16; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = (Math.random() - 0.5D) * Math.PI;
            double dx = Math.cos(theta) * Math.cos(phi) * 0.08D;
            double dy = Math.sin(phi) * 0.08D;
            double dz = Math.sin(theta) * Math.cos(phi) * 0.08D;
            this.world.spawnParticle(EnumParticleTypes.END_ROD,
                    this.posX, this.posY, this.posZ, dx, dy, dz);
        }
    }

    @Override
    protected ItemStack getArrowStack() {
        // 禁止拾取，不掉落任何物品
        return ItemStack.EMPTY;
    }

    @Override
    public boolean getIsCritical() {
        return false;
    }
}
