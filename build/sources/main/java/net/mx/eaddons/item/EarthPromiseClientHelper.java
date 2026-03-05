package net.mx.eaddons.item;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class EarthPromiseClientHelper {
    private static long cooldownEndMillis = 0L;

    public static void setCooldownFromRemainingTicks(int remainingTicks) {
        cooldownEndMillis = System.currentTimeMillis() + remainingTicks * 50L;
    }

    /** Returns remaining cooldown in seconds (0 if not on cooldown). */
    public static int getCooldownRemainingSeconds() {
        long remaining = (cooldownEndMillis - System.currentTimeMillis()) / 1000L;
        return remaining > 0 ? (int) remaining : 0;
    }
}
