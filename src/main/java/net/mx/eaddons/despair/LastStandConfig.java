package net.mx.eaddons.despair;

import net.minecraftforge.common.config.Configuration;

/** 绝望者证章「绝境」的数值，和证章属性同放在 InsigniaOfDespair 分类下。 */
public final class LastStandConfig {
    private static final String CATEGORY = "InsigniaOfDespair";

    public static boolean enabled = true;
    public static boolean hardcoreOnly = true;
    /** 绝境时长（tick），期间不会死亡。 */
    public static int durationTicks = 200;
    /** 冷却（tick），从触发时开始算。 */
    public static int cooldownTicks = 24000;
    /** 结束时生命不低于最大值的这个百分比才算脱险。 */
    public static int surviveHealthPercent = 30;
    /** 绝境中击杀敌对生物时回复的生命（最大值的百分比）。 */
    public static int killHealPercent = 25;
    /** 绝境期间的速度等级，0 为不给。 */
    public static int speedLevel = 2;

    private LastStandConfig() {
    }

    public static void init(Configuration config) {
        enabled = config.getBoolean("LastStandEnabled", CATEGORY, true,
                "Last Stand: when a lethal hit gets past every other death protection, the wearer drops to 1 health "
                        + "and gets a few seconds to kill a hostile creature or heal up, instead of dying.");

        hardcoreOnly = config.getBoolean("LastStandHardcoreOnly", CATEGORY, true,
                "Last Stand only works in Hardcore worlds (the insignia itself is only obtainable there).");

        durationTicks = config.getInt("LastStandDuration", CATEGORY, 200, 20, 1200,
                "Length of Last Stand in ticks. The wearer cannot die during it.");

        cooldownTicks = config.getInt("LastStandCooldown", CATEGORY, 24000, 0, 1728000,
                "Cooldown in ticks, counted from the moment Last Stand triggers. Stored on the player, so swapping "
                        + "insignias does not reset it. 24000 = one Minecraft day (20 minutes).");

        surviveHealthPercent = config.getInt("LastStandSurviveHealthPercent", CATEGORY, 30, 1, 100,
                "Health (percent of max) needed when Last Stand ends to survive. With First Aid, the lowest of "
                        + "the body parts that can cause death (head and body by default) is used.");

        killHealPercent = config.getInt("LastStandKillHealPercent", CATEGORY, 25, 0, 100,
                "Killing a hostile creature (a monster, or anything targeting the wearer) during Last Stand ends it "
                        + "at once and heals this percent of max health.");

        speedLevel = config.getInt("LastStandSpeedLevel", CATEGORY, 2, 0, 5,
                "Speed level granted during Last Stand. 0 disables it.");
    }
}
