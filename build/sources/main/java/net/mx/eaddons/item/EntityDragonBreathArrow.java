package net.mx.eaddons.item;

import com.google.common.collect.Sets;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.mx.eaddons.potion.PotionDragonBreath;

import java.util.List;
import java.util.Set;

public class EntityDragonBreathArrow extends EntityArrow {
    private final Set<PotionEffect> effects = Sets.newHashSet();

    public EntityDragonBreathArrow(World world) {
        super(world);
        this.setDamage(this.getDamage() * 2);
        this.pickupStatus = PickupStatus.DISALLOWED;
    }

    public EntityDragonBreathArrow(World world, EntityLivingBase shooter) {
        super(world, shooter);
        this.setDamage(this.getDamage() * 2);
        this.pickupStatus = PickupStatus.DISALLOWED;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            Vec3d movement = new Vec3d(this.motionX, this.motionY, this.motionZ);
            double dx = movement.x;
            double dy = movement.y;
            double dz = movement.z;
            double length = movement.lengthVector() * 1.25D;
            for (int i = 0; i < (int) length; ++i) {
                this.world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        this.posX + (this.rand.nextDouble() - 0.5) * this.width + dx * (double) i / length,
                        this.posY + this.rand.nextDouble() * this.height + dy * (double) i / length,
                        this.posZ + (this.rand.nextDouble() - 0.5) * this.width + dz * (double) i / length,
                        -dx * 0.1, -dy * 0.1, -dz * 0.1);
            }
            if (!this.hasNoGravity()) {
                this.motionY += 0.02;
            }
        } else if (this.inGround) {
            summonAreaEffect();
        }
    }

    @Override
    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (!this.world.isRemote && result.typeOfHit == RayTraceResult.Type.ENTITY
                && result.entityHit != this.shootingEntity) {
            Vec3d motion = new Vec3d(this.motionX, this.motionY, this.motionZ);
            this.setPosition(this.posX + motion.x * 0.5, this.posY + motion.y * 0.5, this.posZ + motion.z * 0.5);
            summonAreaEffect();
        }
    }

    @Override
    protected void arrowHit(EntityLivingBase living) {
        super.arrowHit(living);
        living.hurtResistantTime = 0;
    }

    private void summonAreaEffect() {
        Vec3d motion = new Vec3d(this.motionX, this.motionY, this.motionZ);
        float speed = (float) motion.lengthVector();
        float dmg = MathHelper.ceil(MathHelper.clamp(Math.sqrt(speed) * this.getDamage(), 0.0, 2.147483647E9)) / 4.0F;

        List<EntityLivingBase> entities = this.world.getEntitiesWithinAABB(
                EntityLivingBase.class, this.getEntityBoundingBox().grow(4.0, 2.0, 4.0));

        EntityAreaEffectCloud effectCloud = new EntityAreaEffectCloud(this.world, this.posX, this.posY, this.posZ);
        if (this.shootingEntity instanceof EntityLivingBase) {
            effectCloud.setOwner((EntityLivingBase) this.shootingEntity);
        }

        effectCloud.setParticle(EnumParticleTypes.DRAGON_BREATH);
        effectCloud.setRadius(1.5F);
        effectCloud.setDuration(100);
        effectCloud.setRadiusOnUse(0.1F);
        effectCloud.setWaitTime(1);
        effectCloud.addEffect(new PotionEffect(PotionDragonBreath.INSTANCE, 1, MathHelper.floor(dmg)));

        if (!this.effects.isEmpty()) {
            for (PotionEffect effect : this.effects) {
                PotionEffect scaled = new PotionEffect(effect.getPotion(), effect.getDuration() / 5, effect.getAmplifier());
                effectCloud.addEffect(scaled);
            }
        }

        if (!entities.isEmpty()) {
            for (EntityLivingBase entity : entities) {
                if (this.getDistanceSq(entity) < 12.0 && entity != this.shootingEntity) {
                    effectCloud.setPosition(entity.posX, entity.posY, entity.posZ);
                    break;
                }
            }
        }

        this.world.spawnEntity(effectCloud);
        this.setDead();
    }

    @Override
    protected ItemStack getArrowStack() {
        return ItemStack.EMPTY;
    }

    public void addEffect(PotionEffect effect) {
        this.effects.add(effect);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (!this.effects.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (PotionEffect effect : this.effects) {
                list.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            }
            compound.setTag("CustomPotionEffects", list);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("CustomPotionEffects", 9)) {
            NBTTagList list = compound.getTagList("CustomPotionEffects", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
                if (effect != null) {
                    this.effects.add(effect);
                }
            }
        }
    }


    @Override
    public boolean getIsCritical() {
        return false;
    }
}
