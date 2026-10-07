package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.mx.eaddons.EAddonsMod;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.DamageSource;
import net.mx.eaddons.compat.ModCompat;
import net.mx.eaddons.util.DamageEstimate;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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

    /**
     * First Aid 按部位结算时每个护甲槽都会发一次 LivingDamageEvent；整次免疫已在下面的 LivingHurtEvent 判过，
     * 这里对这类玩家只再做一次七咒减伤（减伤叠两次，当作特性保留）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        boolean allowTrigger = !ModCompat.firstAidTakesOver(event.getEntityLiving());
        float result = resolve((EntityPlayer) event.getEntity(), event.getSource(), event.getAmount(), false, allowTrigger);
        if (result < 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(result);
        }
    }

    /** 装了 First Aid 时整次免疫在这里判（护甲结算前，每次受伤一次）；LOW 排在 First Aid 的 LOWEST 之前。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!ModCompat.firstAidTakesOver(event.getEntityLiving())) return;
        float result = resolve((EntityPlayer) event.getEntityLiving(), event.getSource(), event.getAmount(), true, true);
        if (result < 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(result);
        }
    }

    /**
     * 七咒减伤 + 大伤害整次免疫。
     *
     * @param preArmor amount 是否为护甲结算前的数值；是则用估算的护甲后伤害比阈值（1.20.1 原版比的就是护甲后伤害）
     * @param allowTrigger false 时只做七咒减伤，不判定整次免疫
     * @return 调整后的伤害；负数表示触发，整次免疫
     */
    private static float resolve(EntityPlayer player, DamageSource source, float amount, boolean preArmor,
                                 boolean allowTrigger) {
        if (!ItemEarthPromise.hasEarthPromise(player)) return amount;

        float damage = amount;
        if (ItemForgerGem.hasCursedRing(player)) {
            damage = damage * (1.0F - EarthPromiseConfig.getFirstCurseResistanceMultiplier());
        }
        if (!allowTrigger || isOnCooldown(player)) {
            return damage;
        }

        float taken = preArmor ? DamageEstimate.taken(player, source, damage) : damage;
        float triggerThreshold = player.getHealth() * (EarthPromiseConfig.abilityTriggerPercent / 100.0F);
        if (player.isEntityAlive() && !source.isUnblockable() && taken >= triggerThreshold) {
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
            return -1.0F;
        }
        return damage;
    }

    // 挖掘加速已改为 eaddons.miningSpeed 属性来源（见 attribute.EAddonsAttributeSources）

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        Long until = COOLDOWN_UNTIL.get(event.player.getUniqueID());
        if (until != null && event.player.world.getTotalWorldTime() >= until) {
            COOLDOWN_UNTIL.remove(event.player.getUniqueID());
        }
    }
}
