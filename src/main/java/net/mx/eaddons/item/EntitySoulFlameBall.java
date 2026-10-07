package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.mx.eaddons.EAddonsMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 灵魂火球：幻影灵魂灯笼生成的追踪型伴随投射物，移植自 1.20 神遗拓展 SoulFlameBall。
 * 平时绕主人环绕；当探测到有生物正以主人为攻击目标时点火，归位飞向该目标并造成魔法伤害。
 * 因 1.12.2 无 Projectile/Targeting，改基于 {@link Entity} 自制归位与碰撞，用 getAttackTarget 判定。
 */
public class EntitySoulFlameBall extends Entity {
    private static final DataParameter<Boolean> FIRED =
            EntityDataManager.createKey(EntitySoulFlameBall.class, DataSerializers.BOOLEAN);

    private int fireTime;
    private int ballId;
    /** 伤害覆盖值，>0 时取代默认的「2+魂等级」公式。共鸣者的幻影形态按攻击力传入。 */
    private float damageOverride;
    @Nullable private UUID ownerUUID;
    @Nullable private Entity cachedOwner;
    @Nullable private UUID targetUUID;
    @Nullable private Entity cachedTarget;

    public EntitySoulFlameBall(World world) {
        super(world);
        setSize(0.5F, 0.5F);
        this.setNoGravity(true);
    }

    public EntitySoulFlameBall(World world, EntityLivingBase owner, int ballId) {
        this(world);
        this.ownerUUID = owner.getUniqueID();
        this.cachedOwner = owner;
        this.ballId = ballId;
        int offset = ballId * 18 + owner.ticksExisted % 90;
        double y = owner.posY + owner.height * 0.4;
        this.setPosition(
                owner.posX + Math.sin(Math.PI / 45.0 * offset),
                y,
                owner.posZ + Math.cos(Math.PI / 45.0 * offset));
    }

    /**
     * 直射构造：供术质共鸣者的幻影形态使用，跳过饰品那套环绕轨道，创建即点火沿指定方向飞出。
     * 飞出后仍会走 {@code checkTarget}，所以照样会自动咬住附近以主人为目标的生物。
     */
    public EntitySoulFlameBall(World world, EntityLivingBase owner, Vec3d velocity, float damage) {
        this(world);
        this.ownerUUID = owner.getUniqueID();
        this.cachedOwner = owner;
        this.damageOverride = damage;
        this.setPosition(owner.posX, owner.posY + owner.getEyeHeight() - 0.1, owner.posZ);
        this.motionX = velocity.x;
        this.motionY = velocity.y;
        this.motionZ = velocity.z;
        setFired(true);
    }

    /** 是否由术质共鸣者射出（只有直射构造会设伤害覆盖值），用于区分饰品环绕的那几颗。 */
    public boolean isShotBySword() {
        return this.damageOverride > 0;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(FIRED, false);
    }

    public boolean isFired() {
        return this.dataManager.get(FIRED);
    }

    private void setFired(boolean fired) {
        this.dataManager.set(FIRED, fired);
    }

    @Nullable
    private Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isDead) return cachedOwner;
        if (ownerUUID != null && world instanceof net.minecraft.world.WorldServer) {
            cachedOwner = ((net.minecraft.world.WorldServer) world).getEntityFromUuid(ownerUUID);
        }
        return cachedOwner;
    }

    @Nullable
    private Entity getCachedTarget() {
        if (cachedTarget != null && !cachedTarget.isDead) return cachedTarget;
        if (targetUUID != null && world instanceof net.minecraft.world.WorldServer) {
            cachedTarget = ((net.minecraft.world.WorldServer) world).getEntityFromUuid(targetUUID);
            return cachedTarget;
        }
        return null;
    }

    private void setTarget(Entity entity) {
        this.targetUUID = entity.getUniqueID();
        this.cachedTarget = entity;
    }

    /** 探测 16 格内是否有正以主人为目标的生物，有则点火飞向它。 */
    private void checkTarget() {
        Entity owner = getOwner();
        if (owner == null || this.ticksExisted < 10) return;
        List<EntityLiving> list = world.getEntitiesWithinAABB(EntityLiving.class,
                getEntityBoundingBox().grow(16.0));
        for (EntityLiving living : list) {
            if (!living.isEntityAlive()) continue;
            if (living.getAttackTarget() == owner) {
                setTarget(living);
                if (!isFired()) {
                    Vec3d dir = new Vec3d(posX - owner.posX, posY - owner.posY + 0.32, posZ - owner.posZ).scale(1.5);
                    this.motionX = dir.x;
                    this.motionY = dir.y;
                    this.motionZ = dir.z;
                }
                setFired(true);
                break;
            }
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        // 客户端只负责粒子；生命周期完全由服务端管理。
        // 客户端拿不到 owner（未同步），若在此判 owner==null 会误杀自身导致火球不可见。
        if (world.isRemote) {
            if (this.rand.nextInt(3) == 0) {
                EAddonsMod.proxy.spawnSoulParticle(world, posX, posY + height * 0.5, posZ, true);
            }
            if (this.rand.nextInt(3) == 0) {
                EAddonsMod.proxy.spawnSoulParticle(world, posX, posY + height * 0.5, posZ, false);
            }
            return;
        }

        Entity owner = getOwner();
        if (this.fireTime > 125 || owner == null) {
            this.setDead();
            return;
        }

        if (isFired()) {
            tickFired();
        } else {
            tickOrbit(owner);
        }
    }

    private void tickFired() {
        Entity target = getCachedTarget();
        if (target != null && target.isEntityAlive()) {
            Vec3d tpos = new Vec3d(target.posX, target.posY + target.height * 0.5, target.posZ);
            Vec3d delta = tpos.subtract(posX, posY, posZ);
            if (delta.lengthVector() < 16.0) {
                delta = delta.normalize().scale(0.56).addVector(motionX, motionY, motionZ);
            } else {
                checkTarget();
            }
            Vec3d move = delta.scale(0.7);
            this.motionX = move.x;
            this.motionY = move.y;
            this.motionZ = move.z;
            double horiz = Math.sqrt(move.x * move.x + move.z * move.z);
            this.rotationYaw = (float) (MathHelper.atan2(-move.x, -move.z) * (180.0 / Math.PI));
            this.rotationPitch = (float) (MathHelper.atan2(move.y, horiz) * (180.0 / Math.PI));
        } else {
            checkTarget();
        }

        // 先移动，再检测碰撞
        this.setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        this.fireTime++;

        Entity hit = findEntityHit();
        if (hit instanceof EntityLivingBase) {
            onHitEntity((EntityLivingBase) hit);
            return;
        }
        if (isBlockedAt()) {
            burstAndDie();
        }
    }

    private void tickOrbit(Entity owner) {
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(10.0));
        if (!players.contains(owner)) {
            this.setDead();
            return;
        }
        checkTarget();
        int offset = ballId * 18 + owner.ticksExisted % 90;
        double y = owner.posY + owner.height * 0.4;
        this.setPosition(
                owner.posX + Math.sin(Math.PI / 45.0 * offset),
                y,
                owner.posZ + Math.cos(Math.PI / 45.0 * offset));
    }

    @Nullable
    private Entity findEntityHit() {
        Entity owner = getOwner();
        AxisAlignedBB box = getEntityBoundingBox().grow(0.3);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase living : list) {
            if (living == owner || !living.isEntityAlive()) continue;
            return living;
        }
        return null;
    }

    private boolean isBlockedAt() {
        BlockPos pos = new BlockPos(posX, posY, posZ);
        if (!world.isBlockLoaded(pos)) return false;
        return world.getBlockState(pos).getMaterial().blocksMovement()
                && world.getBlockState(pos).isFullCube();
    }

    private void onHitEntity(EntityLivingBase target) {
        Entity owner = getOwner();
        if (target == owner) return;
        int soulLevel = target.getEntityData().getInteger("IllusionSoulLevel");
        float maxHealth = owner instanceof EntityLivingBase ? ((EntityLivingBase) owner).getMaxHealth() : 20.0F;
        float damage = this.damageOverride > 0.0F
                ? this.damageOverride
                : Math.min(2.0F + soulLevel, maxHealth * 0.75F);
        DamageSource src = owner != null
                ? DamageSource.causeIndirectMagicDamage(this, owner)
                : DamageSource.MAGIC;
        target.attackEntityFrom(src, damage);
        target.hurtResistantTime = 0;
        target.getEntityData().setInteger("IllusionSoulLevel", soulLevel + 1);
        burstAndDie();
    }

    private void burstAndDie() {
        for (int i = 0; i < 6; i++) {
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.EXPLOSION_NORMAL,
                    posX, posY + height * 0.5, posZ, 0, 0, 0);
        }
        this.setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.fireTime = compound.getInteger("FireTime");
        this.ballId = compound.getInteger("BallID");
        this.damageOverride = compound.getFloat("DamageOverride");
        if (compound.hasUniqueId("OwnerUUID")) this.ownerUUID = compound.getUniqueId("OwnerUUID");
        if (compound.hasUniqueId("TargetUUID")) this.targetUUID = compound.getUniqueId("TargetUUID");
        setFired(compound.getBoolean("Fired"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("FireTime", this.fireTime);
        compound.setInteger("BallID", this.ballId);
        compound.setFloat("DamageOverride", this.damageOverride);
        if (this.ownerUUID != null) compound.setUniqueId("OwnerUUID", this.ownerUUID);
        if (this.targetUUID != null) compound.setUniqueId("TargetUUID", this.targetUUID);
        compound.setBoolean("Fired", isFired());
    }
}
