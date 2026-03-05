package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class EtheriumCoreClientHelper {
    private static long cooldownEndMillis = 0L;
    private static int shieldTicksRemaining = 0;

    public static void setFromSync(int cooldownRemainingTicks, int shieldTicksRemainingIn) {
        cooldownEndMillis = cooldownRemainingTicks > 0 ? System.currentTimeMillis() + cooldownRemainingTicks * 50L : 0L;
        shieldTicksRemaining = Math.max(0, shieldTicksRemainingIn);
    }

    public static void setCooldownFromRemainingTicks(int remainingTicks) {
        cooldownEndMillis = remainingTicks > 0 ? System.currentTimeMillis() + remainingTicks * 50L : 0L;
    }

    public static void setShieldTicksRemaining(int ticks) {
        shieldTicksRemaining = Math.max(0, ticks);
    }

    /** Decrement once per client tick for aura; call from client tick event. */
    public static void tickShield() {
        if (shieldTicksRemaining > 0) shieldTicksRemaining--;
    }

    /** Returns remaining cooldown in seconds (0 if not on cooldown). */
    public static int getCooldownRemainingSeconds() {
        if (cooldownEndMillis <= 0) return 0;
        long remaining = (cooldownEndMillis - System.currentTimeMillis()) / 1000L;
        return remaining > 0 ? (int) remaining : 0;
    }

    public static int getShieldTicksRemaining() {
        return shieldTicksRemaining;
    }

    /** True if active ability shield is up (synced ticks > 0). */
    public static boolean hasShieldActive() {
        return shieldTicksRemaining > 0;
    }

    /** True when full Etherium set + Core and health <= threshold (set-only shield, no active duration). */
    public static boolean hasSetShield(EntityPlayer player) {
        if (player == null || !ItemEtheriumCore.hasEtheriumCore(player)) return false;
        if (!ItemEtheriumCore.hasFullEtheriumSet(player)) return false;
        float threshold = 0.4F * EtheriumCoreConfig.shieldThresholdMultiplier;
        return player.getHealth() / player.getMaxHealth() <= threshold;
    }

    /** True if shield should show aura: active shield or set-only shield. */
    public static boolean hasShieldActive(EntityPlayer player) {
        return hasShieldActive() || hasSetShield(player);
    }
}
