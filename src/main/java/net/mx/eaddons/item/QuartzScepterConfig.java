package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 魔法石英权杖的配置。
 * 移植自 1.20 神遗拓展 QuartzScepter，把原版写死的数值全部挂到配置文件方便调平衡。
 */
public class QuartzScepterConfig {
    private static final String CATEGORY = "QuartzScepter";

    /** 发射匕首的基础间隔（tick）。原版为 8。 */
    public static int shootInterval = 8;
    /** 佩戴魔法石英戒指时，发射间隔减少的 tick 数。 */
    public static int ringIntervalReduction = 2;
    /** 主手攻击力加成。 */
    public static double attackDamage = 1.5;
    /** 主手攻击速度加成（负值表示比拳头慢）。 */
    public static double attackSpeed = -1.6;
    /** 权杖耐久。 */
    public static int durability = 160;
    /** 匕首基础伤害。原版继承原版箭矢默认 2.0。 */
    public static double daggerBaseDamage = 2.0;
    /** 匕首基础飞行速度。 */
    public static double daggerVelocity = 1.2;
    /** 佩戴魔法石英戒指时匕首的飞行速度。 */
    public static double daggerVelocityWithRing = 1.6;
    /** 幸运值转化为匕首额外伤害的系数（luck * factor 向下取整）。 */
    public static double luckDamageFactor = 0.8;
    /** 蓄满释放后的基础冷却（tick）。 */
    public static int baseCooldown = 40;
    /** 蓄满释放时，未持有魔法石英花额外增加的冷却（tick）。 */
    public static int noFlowerCooldownPenalty = 20;
    /** 提前松手时，未持有魔法石英花额外计入的蓄力时长（tick，再按 1/5 折算进冷却）。 */
    public static int releaseNoFlowerPenalty = 50;

    public static void init(Configuration config) {
        shootInterval = config.getInt("ShootInterval", CATEGORY, 8, 1, 40,
                "The base interval (in ticks) between dagger shots while channeling.");

        ringIntervalReduction = config.getInt("RingIntervalReduction", CATEGORY, 2, 0, 39,
                "Ticks subtracted from the shoot interval when a Magic Quartz Ring is worn.");

        attackDamage = config.getFloat("AttackDamage", CATEGORY, 1.5F, 0F, 2048F,
                "Bonus attack damage of the scepter in main hand.");

        attackSpeed = config.getFloat("AttackSpeed", CATEGORY, -1.6F, -4F, 4F,
                "Attack speed modifier of the scepter (vanilla base is 4.0).");

        durability = config.getInt("Durability", CATEGORY, 160, 1, 32768,
                "Durability of the scepter.");

        daggerBaseDamage = config.getFloat("DaggerBaseDamage", CATEGORY, 2.0F, 0F, 2048F,
                "Base damage of each thrown quartz dagger, before luck bonus.");

        daggerVelocity = config.getFloat("DaggerVelocity", CATEGORY, 1.2F, 0.1F, 5F,
                "Base flight velocity of thrown daggers.");

        daggerVelocityWithRing = config.getFloat("DaggerVelocityWithRing", CATEGORY, 1.6F, 0.1F, 5F,
                "Flight velocity of thrown daggers when a Magic Quartz Ring is worn.");

        luckDamageFactor = config.getFloat("LuckDamageFactor", CATEGORY, 0.8F, 0F, 10F,
                "Luck-to-damage factor. Extra dagger damage = floor(luck * factor + 0.5).");

        baseCooldown = config.getInt("BaseCooldown", CATEGORY, 40, 0, 1200,
                "Base cooldown (ticks) after a fully-charged release.");

        noFlowerCooldownPenalty = config.getInt("NoFlowerCooldownPenalty", CATEGORY, 20, 0, 200,
                "Extra cooldown (ticks) on a full release when not holding a Magic Quartz Flower.");

        releaseNoFlowerPenalty = config.getInt("ReleaseNoFlowerPenalty", CATEGORY, 50, 0, 400,
                "Extra channel time (ticks) counted on an early release without a Magic Quartz Flower, "
                        + "then divided by 5 into the cooldown.");
    }

    /** 计入戒指减免后的有效发射间隔，最低 1 tick。 */
    public static int getEffectiveInterval(boolean hasRing) {
        int interval = shootInterval - (hasRing ? ringIntervalReduction : 0);
        return Math.max(1, interval);
    }
}
