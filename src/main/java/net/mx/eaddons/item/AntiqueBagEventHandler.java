package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Event handler for all Antique Bag special effects.
 * Effects are checked by looking at the bag contents and only applied once (no
 * stacking).
 */
public class AntiqueBagEventHandler {

    private static final ResourceLocation THE_ACKNOWLEDGMENT = new ResourceLocation("enigmaticlegacy",
            "the_acknowledgment");
    private static final ResourceLocation THE_TWIST = new ResourceLocation("enigmaticlegacy", "the_twist");
    private static final ResourceLocation THE_INFINITUM = new ResourceLocation("enigmaticlegacy", "the_infinitum");
    private static final ResourceLocation HALF_HEART_MASK = new ResourceLocation("enigmaticlegacy", "half_heart_mask");

    /**
     * the_acknowledgment effect: apply 3 seconds of fire to the attacked target.
     * Uses HIGHEST priority to ensure it runs before other mods can cancel the
     * event.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            if (ItemAntiqueBag.hasItemInBag(player, THE_ACKNOWLEDGMENT)) {
                event.getEntityLiving().setFire(3);
            }
        }
    }

    /**
     * the_twist: +10% damage to Boss and Player targets
     * the_infinitum: +20% damage to Boss and Player targets
     * Uses HIGHEST priority to run before other handlers can cancel.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            EntityLivingBase target = event.getEntityLiving();

            // Only apply damage bonus to Boss mobs and other Players
            boolean isBossOrPlayer = !target.isNonBoss() || target instanceof EntityPlayer;
            if (!isBossOrPlayer)
                return;

            boolean hasInfinitum = ItemAntiqueBag.hasItemInBag(player, THE_INFINITUM);
            boolean hasTwist = ItemAntiqueBag.hasItemInBag(player, THE_TWIST);

            if (hasInfinitum) {
                // +20% damage (the_infinitum takes priority)
                event.setAmount(event.getAmount() * 1.2F);
            } else if (hasTwist) {
                // +10% damage
                event.setAmount(event.getAmount() * 1.1F);
            }
        }
    }

    /**
     * the_infinitum: +10% lifesteal on ALL damage dealt (not just boss/player).
     * Uses HIGHEST priority to run before other handlers can cancel.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;

            boolean hasInfinitum = ItemAntiqueBag.hasItemInBag(player, THE_INFINITUM);

            if (hasInfinitum) {
                float lifesteal = event.getAmount() * 0.1F;
                if (lifesteal > 0) {
                    player.heal(lifesteal);
                }
            }
        }
    }

    /**
     * half_heart_mask: Lock health at 50% of max.
     * Runs every server tick.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.player.world.isRemote)
            return;
        if (!(event.player instanceof EntityPlayerMP))
            return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        ItemAntiqueBag.tickFlowersInBag(player);

        if (!ItemAntiqueBag.hasItemInBag(player, HALF_HEART_MASK))
            return;

        float maxHealth = player.getMaxHealth();
        float halfMax = maxHealth * 0.5F;

        if (Loader.isModLoaded("firstaid")) {
            try {
                AntiqueBagCompatFirstAid.onFirstAidHlfHealth(player);
            } catch (Exception e) {
                // ignored — onLivingHeal still prevents healing above 50%
            }
        } else {
            if (player.getHealth() > halfMax) {
                player.setHealth(halfMax);
            }
        }
    }

    /**
     * half_heart_mask: Cancel healing if health is already at or above 50%.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
            if (ItemAntiqueBag.hasItemInBag(player, HALF_HEART_MASK)) {
                float halfMax = player.getMaxHealth() * 0.5F;
                if (player.getHealth() >= halfMax) {
                    event.setCanceled(true);
                } else {
                    // Allow healing but cap it so health won't exceed 50%
                    float allowedHeal = halfMax - player.getHealth();
                    if (event.getAmount() > allowedHeal) {
                        event.setAmount(allowedHeal);
                    }
                }
            }
        }
    }
}
