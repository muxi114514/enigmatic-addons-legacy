package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 复苏之叶配置。移植自 1.20 神遗拓展 RevivalLeaf 的 onConfig。
 */
public class RevivalLeafConfig {
    private static final String CATEGORY = "RevivalLeaf";

    /** 主动技能冷却（tick）。 */
    public static int cooldown = 320;
    /** 自然回血：每回 0.5HP 所需的 tick 数。 */
    public static int naturalRegenerationTick = 40;
    /** 主动技能作用半径。 */
    public static double abilityRadius = 5.0;
    /** 佩戴者攻击目标时施加的中毒时长（tick）。 */
    public static int poisonTime = 160;
    /** 施加的中毒等级。 */
    public static int poisonLevel = 1;
    /** 主动技能给周围生物的再生时长（tick）。 */
    public static int regenerationTime = 180;
    /** 主动技能给周围生物的再生等级。 */
    public static int regenerationLevel = 1;

    public static void init(Configuration config) {
        cooldown = config.getInt("Cooldown", CATEGORY, 320, 0, 32768,
                "Active ability cooldown for Revival Leaf (ticks). 20 ticks = 1 second.");

        naturalRegenerationTick = config.getInt("NaturalRegenerationTick", CATEGORY, 40, 5, 1200,
                "Ticks required for each 0.5HP of Revival Leaf's natural regeneration.");

        abilityRadius = config.getFloat("AbilityRadius", CATEGORY, 5.0F, 1F, 64F,
                "The effect radius of Revival Leaf's active ability.");

        poisonTime = config.getInt("PoisonTime", CATEGORY, 160, 0, 32768,
                "Ticks of Poison the bearer applies to entities they attack.");

        poisonLevel = config.getInt("PoisonLevel", CATEGORY, 1, 0, 3,
                "Level of Poison the bearer applies to entities they attack.");

        regenerationTime = config.getInt("RegenerationTime", CATEGORY, 180, 0, 32768,
                "Ticks of Regeneration applied to nearby entities when the ability is activated.");

        regenerationLevel = config.getInt("RegenerationLevel", CATEGORY, 1, 0, 3,
                "Level of Regeneration applied to nearby entities when the ability is activated.");
    }
}
