package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.mx.eaddons.EAddonsMod;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EarthPromiseEventHandler {
    /** Cooldown end time (world total world time) per player UUID. */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    public static boolean isOnCooldown(EntityPlayer player) {
        if (player == null || player.world == null) return true;
        Long until = COOLDOWN_UNTIL.get(player.getUniqueID());
        return until != null && player.world.getTotalWorldTime() < until;
    }

    public static void setCooldown(EntityPlayer player) {
        if (player == null) return;
        int ticks = EarthPromiseConfig.cooldownTicks;
        if (ItemForgerGem.hasLostEngine(player)) {
            ticks = (int) (ticks * EarthPromiseConfig.lostEngineCooldownFactor);
        }
        COOLDOWN_UNTIL.put(player.getUniqueID(), player.world.getTotalWorldTime() + ticks);
    }

    /** Used when sending cooldown to client so tooltip shows correct remaining time. */
    public static int getEffectiveCooldownTicks(EntityPlayer player) {
        int ticks = EarthPromiseConfig.cooldownTicks;
        if (ItemForgerGem.hasLostEngine(player)) {
            ticks = (int) (ticks * EarthPromiseConfig.lostEngineCooldownFactor);
        }
        return ticks;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (!ItemEarthPromise.hasEarthPromise(player)) return;

        float damage = event.getAmount();
        float triggerThreshold = player.getHealth() * (EarthPromiseConfig.abilityTriggerPercent / 100.0F);

        if (ItemForgerGem.hasCursedRing(player)) {
            damage = damage * (1.0F - EarthPromiseConfig.getFirstCurseResistanceMultiplier());
        }

        if (isOnCooldown(player)) {
            event.setAmount(damage);
            return;
        }

        if (player.isEntityAlive() && !event.getSource().isUnblockable() && damage >= triggerThreshold) {
            setCooldown(player);
            if (!player.world.isRemote && player instanceof EntityPlayerMP) {
                EAddonsMod.PACKET_HANDLER.sendTo(new EarthPromiseCooldownMessage(getEffectiveCooldownTicks(player)), (EntityPlayerMP) player);
            }
            if (!player.world.isRemote) {
                player.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, player.posX, player.posY, player.posZ, 1, 0, 0, 0);
                for (int i = 0; i < 36; i++) {
                    player.world.spawnParticle(EnumParticleTypes.END_ROD,
                            player.posX, player.posY + 0.5, player.posZ,
                            0.1, 0.1, 0.1, 0);
                }
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_ENDEREYE_DEATH, SoundCategory.PLAYERS, 5.0F, 1.5F);
            }
            event.setCanceled(true);
        } else {
            event.setAmount(damage);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !ItemEarthPromise.hasEarthPromise(player)) return;
        float bonus = EarthPromiseConfig.getBreakSpeedMultiplier();
        event.setNewSpeed(event.getNewSpeed() * (1.0F + bonus));
    }

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer harvester = event.getHarvester();
        if (harvester == null || !ItemEarthPromise.hasEarthPromise(harvester)) return;
        int bonus = EarthPromiseConfig.fortuneBonus;
        if (bonus <= 0 || event.getDrops().isEmpty()) return;
        java.util.List<net.minecraft.item.ItemStack> drops = event.getDrops();
        java.util.List<net.minecraft.item.ItemStack> toAdd = new java.util.ArrayList<>();
        for (net.minecraft.item.ItemStack original : drops) {
            if (original.isEmpty()) continue;
            for (int i = 0; i < bonus; i++) {
                if (harvester.world.rand.nextFloat() < 0.5F) {
                    net.minecraft.item.ItemStack extra = original.copy();
                    extra.setCount(1);
                    toAdd.add(extra);
                }
            }
        }
        drops.addAll(toAdd);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        Long until = COOLDOWN_UNTIL.get(event.player.getUniqueID());
        if (until != null && event.player.world.getTotalWorldTime() >= until) {
            COOLDOWN_UNTIL.remove(event.player.getUniqueID());
        }
    }
}
