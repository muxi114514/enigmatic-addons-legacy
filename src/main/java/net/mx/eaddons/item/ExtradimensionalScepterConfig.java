package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 超维权杖配置。移植自 1.20 神遗拓展 ExtradimensionalScepter，把原版写死的数值挂到配置文件。
 */
public class ExtradimensionalScepterConfig {
    private static final String CATEGORY = "ExtradimensionalScepter";

    /** 运输模式（收纳/放置生物）的冷却（tick）。 */
    public static int transportingCooldown = 280;
    /** 战斗模式长时间连续使用后的过热冷却（tick）。 */
    public static int overheatingCooldown = 150;
    /** 战斗模式大招一次最多封印的敌人数量。 */
    public static int maxCombatCount = 5;
    /** 权杖耐久。 */
    public static int durability = 240;
    /** 主手攻击力加成。 */
    public static double attackDamage = 1.5;
    /** 主手攻击速度加成。 */
    public static double attackSpeed = -1.6;

    public static void init(Configuration config) {
        transportingCooldown = config.getInt("TransportingCooldown", CATEGORY, 280, 200, 32768,
                "Cooldown (ticks) of Transporting Mode (store / release a creature).");

        overheatingCooldown = config.getInt("OverheatCooldown", CATEGORY, 150, 100, 32768,
                "Cooldown (ticks) of Combat Mode after channeling for too long.");

        maxCombatCount = config.getInt("MaxCombatCount", CATEGORY, 5, 1, 64,
                "Max number of enemies banished at once by Combat Mode's ultimate.");

        durability = config.getInt("Durability", CATEGORY, 240, 1, 32768,
                "Durability of the scepter.");

        attackDamage = config.getFloat("AttackDamage", CATEGORY, 1.5F, 0F, 2048F,
                "Bonus attack damage of the scepter in main hand.");

        attackSpeed = config.getFloat("AttackSpeed", CATEGORY, -1.6F, -4F, 4F,
                "Attack speed modifier of the scepter (vanilla base is 4.0).");
    }
}
