package net.mx.eaddons.potion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.mx.eaddons.item.DragonBowConfig;

import javax.annotation.Nullable;
import java.util.List;

public class PotionDragonBreath extends Potion {
    public static final PotionDragonBreath INSTANCE = new PotionDragonBreath();

    public PotionDragonBreath() {
        super(false, 0xdf61ff);
        setRegistryName("eaddons", "dragon_breath");
        setPotionName("effect.eaddons.dragon_breath");
        registerPotionAttributeModifier(
                SharedMonsterAttributes.MOVEMENT_SPEED,
                "744bf4f9-0647-49a9-9f6c-be42d42faeee",
                -0.125D,
                2
        );
    }

    @Override
    public boolean isInstant() {
        return true;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    public void affectEntity(@Nullable Entity source, @Nullable Entity indirectSource,
                             EntityLivingBase target, int amplifier, double health) {
        float damage = (int) (health * (double) (4 << amplifier) + 0.5);

        if (indirectSource == target) {
            damage = damage * DragonBowConfig.getOwnerResistanceMultiplier();
            if (damage <= 0) return;
        }

        if (source == null) {
            target.attackEntityFrom(DamageSource.DRAGON_BREATH, damage);
        } else {
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(source, indirectSource), damage);
        }

        target.motionX = 0;
        target.motionY = 0;
        target.motionZ = 0;
        target.velocityChanged = true;

        AxisAlignedBB searchBox = target.getEntityBoundingBox().grow(4.0, 2.0, 4.0);
        List<EntityAreaEffectCloud> effectClouds = target.world.getEntitiesWithinAABB(
                EntityAreaEffectCloud.class, searchBox);
        if (!effectClouds.isEmpty()) {
            for (EntityAreaEffectCloud cloud : effectClouds) {
                if (cloud.getDistanceSq(target) < cloud.getRadius() * cloud.getRadius()
                        && target != cloud.getOwner()) {
                    Vec3d targetPos = target.getPositionVector();
                    Vec3d cloudPos = cloud.getPositionVector();
                    Vec3d movement = new Vec3d(target.motionX, target.motionY, target.motionZ).scale(0.5D);
                    Vec3d newPos = cloudPos.add(targetPos.subtract(cloudPos).scale(0.5D));
                    cloud.setPosition(newPos.x + movement.x, newPos.y + movement.y, newPos.z + movement.z);
                    break;
                }
            }
        }
    }

    @Override
    public boolean shouldRenderInvText(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderHUD(PotionEffect effect) {
        return true;
    }
}
