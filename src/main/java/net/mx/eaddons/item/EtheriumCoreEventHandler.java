package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.mx.eaddons.EAddonsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EtheriumCoreEventHandler {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> SHIELD_TICKS = new HashMap<>();
    /** Stored damage bonus for next attack (Etherium conversion). */
    private static final Map<UUID, Float> COUNTERATTACK_STORED = new HashMap<>();

    public static boolean isOnCooldown(EntityPlayer player) {
        if (player == null || player.world == null) return true;
        Long until = COOLDOWN_UNTIL.get(player.getUniqueID());
        return until != null && player.world.getTotalWorldTime() < until;
    }

    public static int getCooldownRemainingTicks(EntityPlayer player) {
        if (player == null || player.world == null) return 0;
        Long until = COOLDOWN_UNTIL.get(player.getUniqueID());
        if (until == null) return 0;
        long remaining = until - player.world.getTotalWorldTime();
        return remaining > 0 ? (int) remaining : 0;
    }

    public static void setCooldown(EntityPlayer player) {
        if (player == null) return;
        int ticks = EtheriumCoreConfig.cooldownTicks;
        if (ItemForgerGem.hasLostEngine(player)) {
            ticks = (int) (ticks * EtheriumCoreConfig.lostEngineCooldownFactor);
        }
        COOLDOWN_UNTIL.put(player.getUniqueID(), player.world.getTotalWorldTime() + ticks);
    }

    // 护盾判定统一读 eaddons.etheriumShield 属性（核心的阈值倍率与主动护盾都是属性来源），
    // 结算交给 EL 的以太护盾（减伤、击退、免投射物）。原先这里与 EL 各减一半，满套低血时实际 ×0.25。

    public static int getShieldTicks(EntityPlayer player) {
        return player == null ? 0 : SHIELD_TICKS.getOrDefault(player.getUniqueID(), 0);
    }

    /** Called from EtheriumCoreTriggerMessage.Handler on server. */
    public static void onTrigger(EntityPlayerMP player) {
        if (player.world.isRemote) return;
        if (!ItemEtheriumCore.hasEtheriumCore(player)) return;
        if (isOnCooldown(player)) return;
        setCooldown(player);
        SHIELD_TICKS.put(player.getUniqueID(), EtheriumCoreConfig.shieldDurationTicks);
        int cooldownTicks = getCooldownRemainingTicks(player);
        int shieldTicks = getShieldTicks(player);
        EAddonsMod.PACKET_HANDLER.sendTo(new EtheriumCoreSyncMessage(cooldownTicks, shieldTicks), player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        if (!ItemEtheriumCore.hasEtheriumCore(player)) return;
        UUID uuid = player.getUniqueID();
        Integer ticks = SHIELD_TICKS.get(uuid);
        if (ticks != null && ticks > 0) {
            int next = ticks - 1;
            if (next <= 0) SHIELD_TICKS.remove(uuid);
            else SHIELD_TICKS.put(uuid, next);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.player.getUniqueID();
        COOLDOWN_UNTIL.remove(uuid);
        SHIELD_TICKS.remove(uuid);
        COUNTERATTACK_STORED.remove(uuid);
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGH)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!ItemEtheriumCore.hasEtheriumCore(player)) return;

        String type = event.getSource().damageType;
        boolean immune = DamageSource.CRAMMING.damageType.equals(type)
                || DamageSource.CACTUS.damageType.equals(type)
                || "thorns".equals(type)
                || "explosion".equals(type)
                || "explosion.player".equals(type);
        if (immune) {
            event.setCanceled(true);
        }
        // 护盾免疫投射物由 EL 的以太护盾处理（判定已改读属性）
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer victim = (EntityPlayer) event.getEntityLiving();
            if (ItemEtheriumCore.hasEtheriumCore(victim)) {
                float add = event.getAmount() * EtheriumCoreConfig.damageConversion;
                float current = COUNTERATTACK_STORED.getOrDefault(victim.getUniqueID(), 0F);
                float capped = Math.min(EtheriumCoreConfig.damageConversionCap, current + add);
                COUNTERATTACK_STORED.put(victim.getUniqueID(), capped);
                // 护盾的减伤与击退由 EL 的以太护盾处理（判定已改读属性）
            }
        }

        Entity trueSource = event.getSource().getTrueSource();
        if (trueSource instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) trueSource;
            if (ItemEtheriumCore.hasEtheriumCore(attacker)) {
                Float stored = COUNTERATTACK_STORED.remove(attacker.getUniqueID());
                if (stored != null && stored > 0) {
                    event.setAmount(event.getAmount() + stored);
                }
            }
        }
    }
}
