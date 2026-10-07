package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 焦阳护符配置。移植自 1.20 神遗拓展 ScorchedCharm 的 onConfig。
 */
public class ScorchedCharmConfig {
    private static final String CATEGORY = "ScorchedCharm";

    /** 处于岩浆中时每秒回复的生命值。 */
    public static double lavaHealAmount = 2.0;
    /** 攻击着火目标时的吸血比例（百分比）。 */
    public static int lifestealPercent = 20;
    /** 受到攻击时几率抗伤的概率（百分比，岩浆中翻倍）。 */
    public static int resistanceProbability = 10;

    /** SimpleDifficulty 控温：佩戴时玩家温度不超过该档位（只降不升）。默认 14 = NORMAL 上限。 */
    public static int sdMaxTemperatureLevel = 14;

    public static void init(Configuration config) {
        lavaHealAmount = config.getFloat("LavaHealAmount", CATEGORY, 2.0F, 0F, 100F,
                "Heal amount per second while standing in lava with the charm.");

        lifestealPercent = config.getInt("LifestealModifier", CATEGORY, 20, 0, 100,
                "Lifesteal (percent of damage) when attacking a target on fire.");

        resistanceProbability = config.getInt("ResistanceProbability", CATEGORY, 10, 0, 100,
                "Probability (percent) to fully resist an incoming attack (doubled in lava).");

        sdMaxTemperatureLevel = config.getInt("SDMaxTemperatureLevel", CATEGORY, 14, 0, 25,
                "SimpleDifficulty compat: while worn, the player's temperature never exceeds this "
                        + "level (only lowered, never raised). SD ranges: NORMAL 11-14, HOT 15-19, "
                        + "BURNING 20-25 (heat damage above 22).");
    }

    /** 吸血倍率。 */
    public static float getLifestealModifier() {
        return lifestealPercent / 100.0F;
    }
}
