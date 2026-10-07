package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/** 术质调谐器的数值。 */
public class SpelltunerConfig {
    private static final String CATEGORY = "Spelltuner";

    /** 虚空调谐：受到致命伤害时免死的概率（%）。 */
    public static int voidWardChance = 30;
    /** 虚空调谐：免死后的完全无敌时长（tick）。 */
    public static int voidWardInvulnTicks = 40;
    /** 引擎调谐：按住右键使用的物品用时缩短（%）。上限 50，即速度翻倍。 */
    public static int engineUseTimeReduction = 30;
    /** 冥灯调谐：被佩戴者打过的亡灵可反击他的时长（秒）。 */
    public static int lanternProvokeSeconds = 30;
    /** 魔像调谐：受到的近战伤害减少（%）。 */
    public static int golemMeleeReduction = 10;
    /** 烈焰调谐：近战命中点燃目标的秒数。 */
    public static int blazeIgniteSeconds = 4;
    /** 星云调谐：受到的魔法伤害减少（%）。 */
    public static int nebulaMagicReduction = 25;
    /** 复苏调谐：每秒回复最大生命的百分比，与每秒最少回复量。 */
    public static double revivalRegenPercent = 1.0D;
    public static double revivalRegenMin = 0.5D;
    /** 忘却调谐：冻结近战攻击者的时长（tick）。 */
    public static int frostFreezeTicks = 100;
    /** 天使调谐：在空中时近战伤害提高（%）。 */
    public static int angelAirBonus = 20;
    /** 海洋调谐：身上湿润时受到的伤害减少（%），以及水下呼吸。 */
    public static int oceanWetReduction = 20;
    public static boolean oceanWaterBreathing = true;
    /** 冥灯调谐：受到亡灵的伤害减少（%）。 */
    public static int lanternUndeadReduction = 20;

    public static void init(Configuration config) {
        voidWardChance = config.getInt("VoidWardChance", CATEGORY, 30, 0, 100,
                "Void Pearl tune: chance (%) to survive a lethal hit with 1 health. Rolled before the Totem of "
                        + "Undying, so a successful roll does not use up the totem. Never triggers on /kill or the void.");

        voidWardInvulnTicks = config.getInt("VoidWardInvulnerableTicks", CATEGORY, 40, 0, 200,
                "Void Pearl tune: ticks of full invulnerability after surviving (/kill and the void still apply).");

        engineUseTimeReduction = config.getInt("EngineUseTimeReduction", CATEGORY, 30, 0, 50,
                "Lost Engine tune: anything used by holding right-click (food, potions, bows, charged weapons...) "
                        + "takes this percent less time. 50 = twice as fast.");

        lanternProvokeSeconds = config.getInt("LanternProvokeSeconds", CATEGORY, 30, 0, 600,
                "Illusion Lantern tune: ordinary undead (not bosses) never target the wearer on their own; one the "
                        + "wearer attacks may fight back for this many seconds, refreshed on every hit.");

        golemMeleeReduction = config.getInt("GolemMeleeReduction", CATEGORY, 10, 0, 100,
                "Golem Heart tune: melee damage taken is reduced by this percent (on top of knockback immunity).");

        blazeIgniteSeconds = config.getInt("BlazeIgniteSeconds", CATEGORY, 4, 0, 60,
                "Blazing Core tune: melee hits set the target on fire for this many seconds. 0 = off.");

        nebulaMagicReduction = config.getInt("NebulaMagicReduction", CATEGORY, 25, 0, 100,
                "Eye of the Nebula tune: magic damage taken is reduced by this percent.");

        revivalRegenPercent = config.getFloat("RevivalRegenPercent", CATEGORY, 1.0F, 0F, 100F,
                "Revival Leaf tune: percent of max health restored every second.");

        revivalRegenMin = config.getFloat("RevivalRegenMin", CATEGORY, 0.5F, 0F, 100F,
                "Revival Leaf tune: minimum health restored every second.");

        frostFreezeTicks = config.getInt("FrostFreezeTicks", CATEGORY, 100, 0, 1200,
                "Forgotten Ice tune: ticks of freeze applied to a melee attacker (bosses get less).");

        angelAirBonus = config.getInt("AngelAirBonus", CATEGORY, 20, 0, 500,
                "Angel's Blessing tune: melee damage bonus (%) while the wearer is airborne.");

        oceanWetReduction = config.getInt("OceanWetReduction", CATEGORY, 20, 0, 100,
                "Ocean Stone tune: damage taken is reduced by this percent while the wearer is wet.");

        oceanWaterBreathing = config.getBoolean("OceanWaterBreathing", CATEGORY, true,
                "Ocean Stone tune: the wearer never runs out of air underwater.");

        lanternUndeadReduction = config.getInt("LanternUndeadReduction", CATEGORY, 20, 0, 100,
                "Illusion Lantern tune: damage taken from undead is reduced by this percent.");
    }
}
