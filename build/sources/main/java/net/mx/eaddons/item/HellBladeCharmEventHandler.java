package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Combat event handler for the Hell Blade Charm.
 *
 * Attacker effects:
 *   - Melee damage ×2 (or ×2.5 with berserk emblem at low HP).
 *   - If damage >= target HP × 75% (50% with cursed ring): ×10 damage + heal.
 *
 * Victim effects:
 *   - +10% damage taken (+5% with berserk emblem).
 *   - Berserk emblem's damage resistance is halved.
 *
 * Cursed scroll compat:
 *   The +2 curse levels are provided by embedding Curse of Vanishing and
 *   Curse of Binding into the charm's NBT. SuperpositionHandler.getCurseAmount
 *   naturally counts them through getFullEquipment, so all cursed_scroll
 *   bonuses (damage, mining, regen) and tooltip display are automatic.
 */
public class HellBladeCharmEventHandler {

    private static boolean isMelee(String damageType) {
        return "player".equals(damageType) || "mob".equals(damageType);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity source = event.getSource().getTrueSource();

        // --- Attacker has charm: damage boost ---
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            if (ItemHellBladeCharm.hasHellBladeCharm(player)) {
                float multiplier = ItemHellBladeCharm.getDamageMultiplier(player);

                if (ItemHellBladeCharm.hasBerserkEmblem(player)) {
                    float missingPool = ItemHellBladeCharm.getMissingHealthPool(player);
                    multiplier += missingPool * 1.0F * 0.25F;
                }

                float effective = isMelee(event.getSource().getDamageType()) ? multiplier : multiplier * 0.5F;
                event.setAmount(event.getAmount() * (1.0F + effective));
            }
        }

        // --- Victim has charm: increased damage taken ---
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (ItemHellBladeCharm.hasHellBladeCharm(player)) {
                float bonus = ItemHellBladeCharm.DAMAGE_TAKEN_BONUS;
                if (ItemHellBladeCharm.hasBerserkEmblem(player))
                    bonus *= 0.5F;
                event.setAmount(event.getAmount() * (1.0F + bonus));

                if (ItemHellBladeCharm.hasBerserkEmblem(player)) {
                    float missingPool = ItemHellBladeCharm.getMissingHealthPool(player);
                    float resistance = missingPool * 0.5F;
                    if (resistance > 0 && resistance < 1.0F)
                        event.setAmount(event.getAmount() * (1.0F - resistance / 2) / (1.0F - resistance));
                }
            }
        }
    }

    /**
     * Kill threshold check: if a single hit deals >= 75% (or 50%) of the target's
     * current HP, massively amplify damage and heal the attacker.
     * Runs at LOWEST priority so all other damage modifiers have been applied first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;

        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayer))
            return;

        EntityPlayer player = (EntityPlayer) source;
        if (!ItemHellBladeCharm.hasHellBladeCharm(player))
            return;
        if (!isMelee(event.getSource().getDamageType()))
            return;

        EntityLivingBase victim = event.getEntityLiving();
        float threshold = ItemHellBladeCharm.getKillThreshold(player);
        float healthRequired = victim.getHealth() * threshold;

        if (event.getAmount() >= healthRequired) {
            player.heal(victim.getHealth() * ItemHellBladeCharm.HEAL_MULTIPLIER);
            event.setAmount(Math.min(event.getAmount() * 10.0F, Float.MAX_VALUE / 10.0F));
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 2.0F);
        }
    }
}
