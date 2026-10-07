package net.mx.eaddons.attribute;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 下落速度、跳跃与摔伤免疫的结算。
 * <p>下落与跳跃两端都要算（客户端负责本地玩家的物理），属性已同步到客户端。
 */
public class MovementAttributeHandler {

    /** 时机与 EL 原缓降一致（LivingUpdate HIGHEST）；眼睛在水中时不处理，留给海洋之石的沉浮逻辑 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.motionY >= 0 || player.onGround || player.capabilities.isFlying) {
            return;
        }
        double factor = EnigmaticAttributes.get(player, EnigmaticAttributes.FALL_SPEED);
        if (factor == 1.0D) {
            return;
        }
        BlockPos eye = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        if (player.world.getBlockState(eye).getMaterial() == Material.WATER) {
            return;
        }
        player.motionY *= factor;
    }

    /** HIGH：先于 EL 失落引擎的「起跳额外 +0.1214」，保持原先先乘后加的顺序 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        double factor = EnigmaticAttributes.get(entity, EnigmaticAttributes.JUMP_BOOST);
        if (factor != 1.0D) {
            entity.motionY *= factor;
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource() != DamageSource.FALL) {
            return;
        }
        double threshold = EnigmaticAttributes.get(event.getEntityLiving(), EnigmaticAttributes.FALL_IMMUNITY);
        if (threshold > 0 && event.getAmount() <= threshold) {
            event.setCanceled(true);
        }
    }
}
