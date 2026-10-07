package net.mx.eaddons.primeval;

import net.minecraftforge.common.config.Configuration;

/** 原初立方（进度物品与术石）以及非欧立方主动改版的数值。 */
public class PrimevalConfig {
    private static final String CATEGORY = "PrimevalCube";

    /** 两处「等级 +1」的上限（amplifier，4 = V 级）。 */
    public static int buffLevelCap = 4;

    /** 非欧立方主动：冷却（tick）。 */
    public static int cubeCooldown = 12000;
    /** 非欧立方主动：正面效果延长到的时长（tick）。 */
    public static int cubeBuffDuration = 72000;

    /** 原初立方主动：记录成功后的冷却（tick）。 */
    public static int recordCooldown = 12000;
    /** 原初立方主动：最多记录几个效果。 */
    public static int recordSlots = 3;
    /** 原初立方：持续给予的效果每次续到多长（tick）。 */
    public static int recordEffectDuration = 100;

    /** 原初立方被动：幸运。 */
    public static int spellstoneLuck = 5;
    /** 原初立方被动：时运等级。 */
    public static int spellstoneFortune = 5;

    public static void init(Configuration config) {
        buffLevelCap = config.getInt("BuffLevelCap", CATEGORY, 4, 0, 254,
                "Highest amplifier the '+1 level' effects can reach (0 = level I, 4 = level V). "
                        + "Applies to both the Non-Euclidean Cube active and the Primeval Cube recorded effects.");

        cubeCooldown = config.getInt("CubeCooldown", CATEGORY, 12000, 0, 1000000,
                "Non-Euclidean Cube active cooldown in ticks. Replaces Enigmatic Legacy's own value.");

        cubeBuffDuration = config.getInt("CubeBuffDuration", CATEGORY, 72000, 20, 10000000,
                "Non-Euclidean Cube active: beneficial effects are extended to this many ticks (never shortened).");

        recordCooldown = config.getInt("RecordCooldown", CATEGORY, 12000, 0, 1000000,
                "Primeval Cube active: cooldown in ticks after a new set of effects is recorded. "
                        + "Closing the menu without choosing, or clearing the record, costs no cooldown.");

        recordSlots = config.getInt("RecordSlots", CATEGORY, 3, 1, 9,
                "Primeval Cube active: how many effects can be recorded at once.");

        recordEffectDuration = config.getInt("RecordEffectDuration", CATEGORY, 100, 20, 1200,
                "Primeval Cube: recorded effects are kept up at this duration (ticks) while worn.");

        spellstoneLuck = config.getInt("SpellstoneLuck", CATEGORY, 5, 0, 1024, "Primeval Cube: Luck bonus.");

        spellstoneFortune = config.getInt("SpellstoneFortune", CATEGORY, 5, 0, 100, "Primeval Cube: Fortune bonus.");
    }
}
