package net.mx.eaddons.spellstone;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.SpellstoneSwordConfig;

/**
 * 共鸣者技能冷却的统一入口。所有形态的主动都经由这里设置冷却，
 * 原初共鸣的「全形态冷却 -25%」才有一个唯一的生效点。
 *
 * <p>短冷却走原版 {@code CooldownTracker}（物品格上有遮罩，手感最好）；
 * 长冷却走剑上的自建计时：原版冷却按物品记，一旦挂上 30 秒，这把剑的整个右键
 * （包括解除链接和其它形态的主动）都会被挡住。
 */
public final class SwordCooldown {

    private SwordCooldown() {
    }

    /** 按当前共鸣折算后的 tick 数。 */
    public static int scale(ItemStack sword, int ticks) {
        if (ticks <= 0 || !SpellstoneData.isPrimevalActive(sword)) {
            return ticks;
        }
        return Math.max(1, (int) Math.round(ticks * SpellstoneSwordConfig.primevalCooldownFactor));
    }

    /** 原版冷却（短冷却用）。 */
    public static void set(EntityPlayer player, ItemStack sword, int ticks) {
        player.getCooldownTracker().setCooldown(sword.getItem(), scale(sword, ticks));
    }

    /** 自建计时（长冷却用）。 */
    public static void start(ItemStack sword, String key, int ticks) {
        SpellstoneData.setTimer(sword, key, scale(sword, ticks));
    }

    public static int remaining(ItemStack sword, String key) {
        return SpellstoneData.getTimer(sword, key);
    }

    public static boolean ready(ItemStack sword, String key) {
        return SpellstoneData.getTimer(sword, key) <= 0;
    }
}
