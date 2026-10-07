package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 忘却冰晶配置。数值取自 1.20 神遗拓展 ForgottenIce，冻结相关项按 1.12.2 的自建冻结重新定义。
 */
public class ForgottenIceConfig {
    private static final String CATEGORY = "ForgottenIce";

    /** 主动技能冷却（tick）。 */
    public static int cooldown = 240;
    /** 主动技能作用半径。 */
    public static double abilityRadius = 5.0D;
    /** 主动技能的固定基础伤害，实际伤害 = 该值 + 佩戴者攻击力。 */
    public static double abilityDamageBase = 2.0D;
    /** 主动技能施加的冻结时长（tick）。 */
    public static int abilityFrostTime = 160;

    /** 佩戴者近战命中时施加的冻结时长（tick）。 */
    public static int meleeFrostTime = 120;
    /** BOSS 的冻结时长除以该值。 */
    public static int bossFrostDivisor = 2;
    /** 按最大生命判定 BOSS 的阈值，0 = 不按血量判定。 */
    public static double bossHealthThreshold = 150.0D;
    /** 额外按注册名判定为 BOSS 的实体。 */
    public static String[] bossEntities = new String[0];

    /** 冻结期间的减速百分比。 */
    public static int slowdownPercent = 75;
    /** 对处于冻结的目标的增伤百分比。 */
    public static int frozenDamageBonus = 30;
    /** 佩戴者受弹射物伤害的减免百分比。 */
    public static int projectileResistance = 50;
    /** 佩戴者受火焰伤害的倍率。 */
    public static double fireVulnerability = 2.5D;
    /** 佩戴者受摔落伤害的倍率。 */
    public static double fallVulnerability = 1.6D;
    /** 冰霜行者等级，0 = 关闭。 */
    public static int frostWalkerLevel = 2;

    private static final String[] DEFAULT_BOSS_ENTITIES = new String[]{
            "iceandfire:icedragon",
            "iceandfire:firedragon",
            "iceandfire:lightningdragon",
            "iceandfire:cyclops",
            "iceandfire:seaserpent",
            "lycanitesmobs:rahovart",
            "lycanitesmobs:asmodeus",
            "lycanitesmobs:amalgalich"
    };

    public static void init(Configuration config) {
        cooldown = config.getInt("Cooldown", CATEGORY, 240, 0, 32768,
                "Active ability cooldown for Forgotten Ice Crystal (ticks). 20 ticks = 1 second.");

        abilityRadius = config.getFloat("AbilityRadius", CATEGORY, 5.0F, 1F, 64F,
                "The effect radius of Forgotten Ice Crystal's active ability.");

        abilityDamageBase = config.getFloat("AbilityDamageBase", CATEGORY, 2.0F, 0F, 1000F,
                "Flat damage of the active ability. Final damage = this value + bearer's attack damage.");

        abilityFrostTime = config.getInt("AbilityFrostTime", CATEGORY, 160, 0, 32768,
                "Frost duration applied by the active ability (ticks).");

        meleeFrostTime = config.getInt("MeleeFrostTime", CATEGORY, 120, 0, 32768,
                "Frost duration applied by the bearer's melee hits (ticks).");

        bossFrostDivisor = config.getInt("BossFrostDivisor", CATEGORY, 2, 1, 16,
                "Frost duration on bosses is divided by this value.");

        bossHealthThreshold = config.getFloat("BossHealthThreshold", CATEGORY, 150.0F, 0F, 10000F,
                "Entities with at least this much max health count as bosses. 0 disables this check.");

        bossEntities = config.getStringList("BossEntities", CATEGORY, DEFAULT_BOSS_ENTITIES,
                "Extra entities treated as bosses, by registry name. Requires game restart.");

        slowdownPercent = config.getInt("SlowdownPercent", CATEGORY, 75, 0, 99,
                "Movement speed reduction while frozen, in percent.");

        frozenDamageBonus = config.getInt("FrozenDamageBonus", CATEGORY, 30, 0, 1000,
                "Bonus damage the bearer deals to frozen targets, in percent.");

        projectileResistance = config.getInt("ProjectileResistance", CATEGORY, 50, 0, 100,
                "Projectile damage reduction for the bearer, in percent.");

        fireVulnerability = config.getFloat("FireVulnerability", CATEGORY, 2.5F, 1F, 10F,
                "Multiplier applied to fire damage taken by the bearer.");

        fallVulnerability = config.getFloat("FallVulnerability", CATEGORY, 1.6F, 1F, 10F,
                "Multiplier applied to fall damage taken by the bearer.");

        frostWalkerLevel = config.getInt("FrostWalkerLevel", CATEGORY, 2, 0, 4,
                "Frost Walker level granted to the bearer. 0 disables it.");
    }
}
