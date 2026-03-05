package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.mx.eaddons.potion.PotionIchorCorrosion;

import java.lang.reflect.Field;

/**
 * Event handler for The Bless weapon effects:
 * - Fourth curse fix: always deal full damage when holding The Bless
 * - Fire bonus: extra damage to burning targets
 * - Ichor corrosion damage amplification: +10% per level
 * - Fire immunity and extended invulnerability frames
 */
public class TheBlessEventHandler {

    private static final int EXTRA_INVULN_TICKS = 10;

    private static float cachedMonsterDamageDebuff = -1F;
    private static Field entityFireField;

    static {
        try {
            entityFireField = Entity.class.getDeclaredField("fire");
            entityFireField.setAccessible(true);
        } catch (Exception e) {
            try {
                entityFireField = Entity.class.getDeclaredField("field_70151_c");
                entityFireField.setAccessible(true);
            } catch (Exception e2) {
                entityFireField = null;
            }
        }
    }

    private static float getMonsterDamageDebuff() {
        if (cachedMonsterDamageDebuff >= 0) return cachedMonsterDamageDebuff;
        try {
            Class<?> configClass = Class.forName("keletu.enigmaticlegacy.EnigmaticConfigs");
            Field field = configClass.getField("monsterDamageDebuff");
            cachedMonsterDamageDebuff = field.getFloat(null);
        } catch (Exception e) {
            cachedMonsterDamageDebuff = 0.5F;
        }
        return cachedMonsterDamageDebuff;
    }

    private static boolean hasCursedRing(EntityPlayer player) {
        try {
            return keletu.enigmaticlegacy.event.SuperpositionHandler.hasCursed(player);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isHoldingTheBless(EntityPlayer player) {
        ItemStack mainhand = player.getHeldItemMainhand();
        return !mainhand.isEmpty() && mainhand.getItem() instanceof ItemTheBless;
    }

    private static boolean isHoldingTheBlessEitherHand(EntityPlayer player) {
        ItemStack mainhand = player.getHeldItemMainhand();
        ItemStack offhand = player.getHeldItemOffhand();
        return (!mainhand.isEmpty() && mainhand.getItem() instanceof ItemTheBless)
                || (!offhand.isEmpty() && offhand.getItem() instanceof ItemTheBless);
    }

    private static int getFireTicks(Entity entity) {
        if (entityFireField != null) {
            try {
                return entityFireField.getInt(entity);
            } catch (Exception ignored) {
            }
        }
        return entity.isBurning() ? 200 : 0;
    }

    /**
     * Cancel fire/lava damage for players holding The Bless.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!isHoldingTheBlessEitherHand(player)) return;

        DamageSource source = event.getSource();
        if (source.isFireDamage()) {
            event.setCanceled(true);
        }
    }

    /**
     * At LOW priority (after EnigmaticEvents NORMAL), undo the 4th curse monster damage debuff
     * for players holding The Bless. Also apply fire bonus damage.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        Entity source = event.getSource().getTrueSource();
        Entity immediate = event.getSource().getImmediateSource();
        if (!(source instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) source;
        if (!isHoldingTheBless(player)) return;

        EntityLivingBase target = event.getEntityLiving();

        // Fire damage bonus: scale with target's remaining fire ticks
        if (target.isBurning()) {
            int fireTicks = getFireTicks(target);
            float fireBonus = Math.min(1.0F, fireTicks * 0.0015F);
            event.setAmount(event.getAmount() * (1.0F + fireBonus));
        }

        // Fourth curse fix: undo the monster damage debuff applied by EnigmaticEvents
        // The debuff is applied when: hasCursed AND (immediate != player OR weapon not whitelisted)
        // Since TheBless is not in the whitelist, the debuff was applied. Undo it.
        if (hasCursedRing(player) && immediate == player) {
            boolean isMonster = target instanceof EntityMob || target instanceof EntityDragon
                    || target instanceof EntityWither;
            if (isMonster) {
                float debuff = getMonsterDamageDebuff();
                if (debuff > 0 && debuff < 1) {
                    event.setAmount(event.getAmount() / (1.0F - debuff));
                }
            }
        }
    }

    /**
     * Handle ichor corrosion damage amplification and invulnerability frame extension.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        EntityLivingBase victim = event.getEntityLiving();

        // Ichor corrosion: +10% damage taken per level
        PotionEffect corrosion = victim.getActivePotionEffect(PotionIchorCorrosion.INSTANCE);
        if (corrosion != null) {
            int amplifier = corrosion.getAmplifier() + 1;
            event.setAmount(event.getAmount() * (1.0F + amplifier * 0.1F));
        }

        // Extend invulnerability frames for The Bless holder when taking damage
        if (victim instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) victim;
            if (isHoldingTheBlessEitherHand(player)) {
                player.hurtResistantTime = Math.max(player.hurtResistantTime,
                        player.maxHurtResistantTime + EXTRA_INVULN_TICKS);
            }
        }
    }

    /**
     * Fire immunity: clear fire every tick while holding The Bless.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        if (isHoldingTheBlessEitherHand(player) && player.isBurning()) {
            player.extinguish();
        }
    }
}
