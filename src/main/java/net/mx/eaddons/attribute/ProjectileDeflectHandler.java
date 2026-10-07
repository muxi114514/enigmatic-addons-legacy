package net.mx.eaddons.attribute;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.packet.PacketForceArrowRotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * 投射物偏转：玩家被射中时按属性值概率把投射物原路弹回。
 * <p>反弹表现照搬 EL 原处理（速度取反、改归属、同步朝向包、偏转音效），EL 那份已被 mixin 停用。
 */
public class ProjectileDeflectHandler {

    /** 与 EL 共用的防循环标记：自己弹回的投射物 10 tick 内再命中自己不再判定 */
    private static final String TAG = "AB_DEFLECTED";

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult result = event.getRayTraceResult();
        if (result == null || !(result.entityHit instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) result.entityHit;
        if (player.world.isRemote) {
            return;
        }
        double chance = EnigmaticAttributes.get(player, EnigmaticAttributes.PROJECTILE_DEFLECT);
        if (chance <= 0) {
            return;
        }
        Entity projectile = event.getEntity();
        if (isOwnedBy(projectile, player) && deflectedRecently(projectile)) {
            // 此处若取消事件，投射物会卡进无限反弹
            return;
        }
        if (player.getRNG().nextDouble() > chance) {
            return;
        }
        event.setCanceled(true);
        deflect(projectile, player);
    }

    private static boolean isOwnedBy(Entity projectile, EntityPlayer player) {
        return (projectile instanceof EntityArrow && ((EntityArrow) projectile).shootingEntity == player)
                || (projectile instanceof EntityFireball && ((EntityFireball) projectile).shootingEntity == player)
                || (projectile instanceof EntityThrowable && ((EntityThrowable) projectile).getThrower() == player);
    }

    private static boolean deflectedRecently(Entity projectile) {
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG + ":")) {
                try {
                    int time = Integer.parseInt(tag.substring(TAG.length() + 1));
                    if (projectile.ticksExisted - time < 10) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    // 别的模组写了同前缀的标记，按未偏转处理
                }
            }
        }
        return false;
    }

    private static void deflect(Entity projectile, EntityPlayer player) {
        projectile.motionX *= -1.0D;
        projectile.motionY *= -1.0D;
        projectile.motionZ *= -1.0D;
        projectile.rotationYaw += 180.0F;
        projectile.prevRotationYaw = projectile.rotationYaw;
        projectile.velocityChanged = true;

        if (projectile instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) projectile;
            arrow.shootingEntity = player;
            arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
        } else if (projectile instanceof EntityFireball) {
            EntityFireball fireball = (EntityFireball) projectile;
            fireball.shootingEntity = player;
            fireball.accelerationX *= -1.0D;
            fireball.accelerationY *= -1.0D;
            fireball.accelerationZ *= -1.0D;
        }

        projectile.getTags().removeIf(tag -> tag.startsWith(TAG));
        projectile.addTag(TAG + ":" + projectile.ticksExisted);

        EnigmaticLegacy.packetInstance.sendToAllAround(
                new PacketForceArrowRotations(projectile.getEntityId(), projectile.rotationYaw, projectile.rotationPitch,
                        projectile.motionX, projectile.motionY, projectile.motionZ,
                        projectile.posX, projectile.posY, projectile.posZ),
                new NetworkRegistry.TargetPoint(player.world.provider.getDimension(),
                        player.posX, player.posY, player.posZ, 64.0D));
        player.world.playSound(null, player.getPosition(), EnigmaticLegacy.DEFLECT_SOUND, SoundCategory.PLAYERS,
                1.0F, 0.95F + player.getRNG().nextFloat() * 0.1F);
    }
}
