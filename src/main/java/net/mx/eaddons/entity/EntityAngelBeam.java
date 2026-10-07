package net.mx.eaddons.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.mx.eaddons.spellstone.SpellstoneAbilities;
import net.mx.eaddons.spellstone.SpellstoneDamage;

/**
 * 天使光束（术质共鸣者·天使形态的投射物，移植自 EL+ AngelBeam）。
 *
 * <p>1.20.1 用 {@code AbstractHurtingProjectile}，1.12.2 这边取 {@link EntityThrowable} 并把重力清零，
 * 碰撞判定与所有者追踪都由父类提供。发射起点同步到客户端供渲染器画光束——
 * 1.12.2 没有 VECTOR3 数据序列化器，拆成三个 float。
 */
public class EntityAngelBeam extends EntityThrowable {

    private static final DataParameter<Float> FROM_X =
            EntityDataManager.createKey(EntityAngelBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FROM_Y =
            EntityDataManager.createKey(EntityAngelBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FROM_Z =
            EntityDataManager.createKey(EntityAngelBeam.class, DataSerializers.FLOAT);

    /** 命中伤害，由发射时按佩戴者攻击力写入。 */
    private float damage = 6.0F;
    /** 是否标记天使祝福（共鸣满级时才标）。 */
    private boolean markBless;

    public EntityAngelBeam(World world) {
        super(world);
        setSize(0.3F, 0.3F);
    }

    public EntityAngelBeam(World world, EntityLivingBase owner, float velocity, float damage, boolean markBless) {
        super(world, owner);
        setSize(0.3F, 0.3F);
        this.damage = damage;
        this.markBless = markBless;
        // 从眼睛位置出发，沿视线方向直射
        this.setPosition(owner.posX, owner.posY + owner.getEyeHeight() - 0.1D, owner.posZ);
        this.shoot(owner, owner.rotationPitch, owner.rotationYaw, 0.0F, velocity, 0.0F);
        this.dataManager.set(FROM_X, (float) this.posX);
        this.dataManager.set(FROM_Y, (float) this.posY);
        this.dataManager.set(FROM_Z, (float) this.posZ);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(FROM_X, 0.0F);
        this.dataManager.register(FROM_Y, 0.0F);
        this.dataManager.register(FROM_Z, 0.0F);
    }

    /** 发射起点（世界坐标），渲染器据此画整条光束。 */
    public Vec3d getBeginning() {
        return new Vec3d(this.dataManager.get(FROM_X), this.dataManager.get(FROM_Y), this.dataManager.get(FROM_Z));
    }

    /** 无重力：光束走直线。 */
    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            double len = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
            if (len > 0.5D) {
                double x = this.posX, y = this.posY, z = this.posZ;
                for (double i = 0.0D; i < len; ) {
                    this.world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z,
                            this.motionX * 0.05D, this.motionY * 0.05D, this.motionZ * 0.05D);
                    double step = 0.5D + this.rand.nextDouble() * 0.3D;
                    i += step;
                    x -= this.motionX * step / len;
                    y -= this.motionY * step / len;
                    z -= this.motionZ * step / len;
                }
            }
        }
        if (this.ticksExisted >= 48) {
            this.setDead();
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote) {
            return;
        }
        if (result.entityHit instanceof EntityLivingBase && result.entityHit != this.getThrower()) {
            EntityLivingBase target = (EntityLivingBase) result.entityHit;
            EntityLivingBase owner = this.getThrower();
            target.attackEntityFrom(SpellstoneDamage.magic(owner == null ? this : owner), this.damage);
            if (this.markBless) {
                SpellstoneAbilities.markBless(target, 2);   // 光束来源：bit2
            }
        }
        this.setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setFloat("BeamDamage", this.damage);
        tag.setBoolean("BeamBless", this.markBless);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.damage = tag.getFloat("BeamDamage");
        this.markBless = tag.getBoolean("BeamBless");
    }
}
