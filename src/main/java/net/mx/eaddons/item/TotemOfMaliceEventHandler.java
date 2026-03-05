package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.network.play.server.SPacketUpdateHealth;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TotemOfMaliceEventHandler {

    /** Half-heart (1 HP): immediate survival so player does not die / lose inventory. */
    private static final float SURVIVAL_HEALTH = 1.0F;
    /** Ticks to wait before applying full heal after totem save. 20 = 1 second. */
    private static final int FULL_HEAL_DELAY_TICKS = 20;
    /** Extra HP granted on delayed heal (on top of max health). */
    private static final float DELAYED_HEAL_BONUS_HP = 1000.0F;
    /** Ticks to keep sending vanilla health packet so client bar wins over First Aid's tick overwrite. */
    private static final int VANILLA_HEALTH_SYNC_TICKS = 5;

    /** Player UUID -> world time when to apply full heal. */
    private static final Map<UUID, Long> pendingFullHeal = new HashMap<>();
    /** Player UUID -> target health to send. First Aid overwrites HEALTH each tick; we re-send so client bar matches. */
    private static final Map<UUID, Float> pendingVanillaHealthSyncHealth = new HashMap<>();
    /** Player UUID -> world time (long) until we stop sending. */
    private static final Map<UUID, Long> pendingVanillaHealthSyncUntil = new HashMap<>();

    private static boolean isIllager(Entity entity) {
        if (entity == null) return false;
        Set<net.minecraft.util.ResourceLocation> ids = TotemOfMaliceConfig.getIllagerEntityIds();
        if (!ids.isEmpty()) {
            net.minecraft.util.ResourceLocation key = net.minecraft.entity.EntityList.getKey(entity);
            return key != null && ids.contains(key);
        }
        return entity instanceof AbstractIllager;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity sourceEntity = event.getSource().getTrueSource();

        // Attacker has totem, target is illager: increase damage
        if (sourceEntity instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) sourceEntity;
            if (ItemTotemOfMalice.hasTotemOfMalice(attacker) && isIllager(event.getEntityLiving())) {
                float mult = 1.0F + TotemOfMaliceConfig.damageBonusPercent / 100.0F;
                event.setAmount(event.getAmount() * mult);
            }
        }

        // Victim has totem, source is illager: reduce damage
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer victim = (EntityPlayer) event.getEntityLiving();
            if (ItemTotemOfMalice.hasTotemOfMalice(victim) && isIllager(sourceEntity)) {
                float mult = 1.0F - TotemOfMaliceConfig.damageReductionPercent / 100.0F;
                event.setAmount(event.getAmount() * mult);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer))
            return;
        if (event.getEntityLiving().world.isRemote)
            return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack totemStack = ItemTotemOfMalice.getTotemStack(player);
        if (totemStack.isEmpty() || ItemTotemOfMalice.getRemainingUses(totemStack) <= 0)
            return;

        event.setCanceled(true);

        player.setHealth(SURVIVAL_HEALTH);
        long deadline = player.world.getTotalWorldTime() + FULL_HEAL_DELAY_TICKS;
        pendingFullHeal.put(player.getUniqueID(), deadline);

        float maxHp = player.getMaxHealth();
        int curseCount = ItemTotemOfMalice.getCurseCount(totemStack);
        float curseMult = 1.0F + (TotemOfMaliceConfig.curseBonusPercent / 100.0F) * curseCount;
        float damage = maxHp * TotemOfMaliceConfig.deathBurstMultiplier * curseMult;

        double r = TotemOfMaliceConfig.aoeRadius;
        AxisAlignedBB box = player.getEntityBoundingBox().grow(r, r, r);
        List<EntityLivingBase> nearby = player.world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        DamageSource ds = DamageSource.causeIndirectMagicDamage(player, player);

        for (EntityLivingBase target : nearby) {
            if (target == player || !target.isEntityAlive()) continue;
            target.attackEntityFrom(ds, damage);
        }

        ItemTotemOfMalice.consumeOneUse(totemStack);

        if (!player.world.isRemote) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            for (int i = 0; i < 20; i++) {
                player.world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                        player.posX, player.posY + 1.0, player.posZ,
                        0.5, 0.5, 0.5, 0);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        for (net.minecraft.entity.player.EntityPlayerMP p : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            Long deadline = pendingFullHeal.get(p.getUniqueID());
            if (deadline != null && p.world.getTotalWorldTime() >= deadline) {
                pendingFullHeal.remove(p.getUniqueID());
                float targetHealth = p.getMaxHealth() + DELAYED_HEAL_BONUS_HP;
                p.setHealth(targetHealth);
                if (AntiqueBagCompatFirstAid.isAvailable()) {
                    AntiqueBagCompatFirstAid.fullHealFirstAid(p);
                }
                // First Aid's tick overwrites HEALTH from body parts and syncs to client; send vanilla packet with target value for several ticks so client bar matches.
                long sendUntil = p.world.getTotalWorldTime() + VANILLA_HEALTH_SYNC_TICKS;
                pendingVanillaHealthSyncHealth.put(p.getUniqueID(), targetHealth);
                pendingVanillaHealthSyncUntil.put(p.getUniqueID(), sendUntil);
            }
        }

        Iterator<UUID> it = pendingVanillaHealthSyncUntil.keySet().iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            EntityPlayerMP q = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(uuid);
            if (q == null || q.connection == null) {
                it.remove();
                pendingVanillaHealthSyncHealth.remove(uuid);
                continue;
            }
            Long sendUntil = pendingVanillaHealthSyncUntil.get(uuid);
            if (sendUntil == null || q.world.getTotalWorldTime() >= sendUntil) {
                it.remove();
                pendingVanillaHealthSyncHealth.remove(uuid);
                continue;
            }
            Float health = pendingVanillaHealthSyncHealth.get(uuid);
            if (health != null) {
                q.connection.sendPacket(new SPacketUpdateHealth(
                        health,
                        q.getFoodStats().getFoodLevel(),
                        q.getFoodStats().getSaturationLevel()));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.player.getUniqueID();
        pendingFullHeal.remove(uuid);
        pendingVanillaHealthSyncHealth.remove(uuid);
        pendingVanillaHealthSyncUntil.remove(uuid);
    }
}
