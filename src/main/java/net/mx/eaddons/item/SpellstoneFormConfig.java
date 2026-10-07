package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 术质共鸣者各形态的强度数值（2026-10 加强方案），与 {@link SpellstoneSwordConfig} 同属 SpellstoneSword 分类。
 * 基础属性、充能与两个立方的数值仍在那边。
 */
public class SpellstoneFormConfig {
    private static final String CATEGORY = "SpellstoneSword";

    // ── 魔像 ──
    public static double golemArmorBonus = 0.6D;
    public static double golemDamageTaken = 0.75D;
    // ── 烈焰 ──
    public static double blazingPreMaxHeatFactor = 0.5D;
    public static double blazingFlameRatio = 0.8D;
    public static double blazingBurstRatio = 2.0D;
    // ── 忘却 ──
    public static double frostShieldRatio = 0.5D;
    public static int frostShieldMin = 2;
    public static double frostCritDivisor = 360.0D;
    // ── 复苏 ──
    public static double revivalUndeadRatio = 0.4D;
    public static double revivalPoisonedRatio = 0.2D;
    // ── 天使 ──
    public static double angelBeamRatio = 1.6D;
    public static double angelBurstRatio = 2.4D;
    // ── 引擎 ──
    public static double engineMomentumStep = 0.05D;
    public static double engineFullStrike = 3.0D;
    public static double engineHookDamageRatio = 1.0D;
    // ── 虚幻 ──
    public static double illusionExecutePerSoul = 0.01D;
    public static double illusionExecuteCap = 0.3D;
    public static double illusionExecuteBonus = 0.08D;
    public static double illusionFlameRatio = 1.5D;
    // ── 星云 ──
    public static double nebulaBlinkRatio = 2.0D;
    public static double nebulaPathRatio = 0.8D;
    // ── 虚空 ──
    public static double voidParryRatio = 1.5D;
    public static double voidParryReflect = 0.5D;
    public static double voidPurgeRatio = 0.15D;
    public static boolean primevalVoidParryNoCooldown = true;
    // ── 非欧 ──
    public static double cubeVoidRatio = 0.5D;
    // ── 通用 ──
    public static boolean aoeSparesAllies = true;

    public static void init(Configuration config) {
        golemArmorBonus = config.getFloat("GolemArmorBonus", CATEGORY, 0.6F, 0F, 10F,
                "Golem Heart form: extra damage per point of the target's armor (added before armor).");
        golemDamageTaken = config.getFloat("GolemDamageTaken", CATEGORY, 0.75F, 0F, 1F,
                "Golem Heart form: multiplier on melee and projectile damage taken while holding the sword.");

        blazingPreMaxHeatFactor = config.getFloat("BlazingPreMaxHeatFactor", CATEGORY, 0.5F, 0F, 1F,
                "Blazing Core form below max level: heat builds up from level 1, its damage bonus scaled by this.");
        blazingFlameRatio = config.getFloat("BlazingFlameRatio", CATEGORY, 0.8F, 0F, 20F,
                "Blazing Core flame breath damage, as a multiplier of attack damage (every 5 ticks).");
        blazingBurstRatio = config.getFloat("BlazingBurstRatio", CATEGORY, 2.0F, 0F, 20F,
                "Blazing Core lava burst damage, as a multiplier of attack damage.");

        frostShieldRatio = config.getFloat("FrostShieldRatio", CATEGORY, 0.5F, 0F, 10F,
                "Forgotten Ice form: ice shield gained per melee hit, as a fraction of the damage dealt.");
        frostShieldMin = config.getInt("FrostShieldMin", CATEGORY, 2, 0, 100,
                "Forgotten Ice form: minimum ice shield gained per melee hit.");
        frostCritDivisor = config.getFloat("FrostCritDivisor", CATEGORY, 360F, 1F, 100000F,
                "Forgotten Ice form: critical hits gain +(target frost ticks / this) damage multiplier.");

        revivalUndeadRatio = config.getFloat("RevivalUndeadRatio", CATEGORY, 0.4F, 0F, 10F,
                "Revival Leaf form at max level: extra magic damage against undead, as a fraction of the hit.");
        revivalPoisonedRatio = config.getFloat("RevivalPoisonedRatio", CATEGORY, 0.2F, 0F, 10F,
                "Revival Leaf form at max level: extra magic damage against poisoned living targets. "
                        + "(The original design healed living targets instead.)");

        angelBeamRatio = config.getFloat("AngelBeamRatio", CATEGORY, 1.6F, 0F, 20F,
                "Angel's Blessing beam damage, as a multiplier of attack damage.");
        angelBurstRatio = config.getFloat("AngelBurstRatio", CATEGORY, 2.4F, 0F, 20F,
                "Angel's Blessing sneaking cone burst damage, as a multiplier of attack damage.");

        engineMomentumStep = config.getFloat("EngineMomentumStep", CATEGORY, 0.05F, 0F, 1F,
                "Lost Engine form: damage bonus per unit of (fall distance x2 + speed x10)^0.9.");
        engineFullStrike = config.getFloat("EngineFullStrike", CATEGORY, 3.0F, 1F, 20F,
                "Lost Engine form at max level: damage multiplier of the full-energy strike.");
        engineHookDamageRatio = config.getFloat("EngineHookDamageRatio", CATEGORY, 1.0F, 0F, 20F,
                "Lost Engine hook damage when it hooks a creature, as a multiplier of attack damage.");

        illusionExecutePerSoul = config.getFloat("IllusionExecutePerSoul", CATEGORY, 0.01F, 0F, 1F,
                "Illusion Lantern form: execute threshold grows by this fraction of max health per soul level.");
        illusionExecuteCap = config.getFloat("IllusionExecuteCap", CATEGORY, 0.3F, 0F, 1F,
                "Illusion Lantern form: cap of the execute threshold, as a fraction of max health.");
        illusionExecuteBonus = config.getFloat("IllusionExecuteBonus", CATEGORY, 0.08F, 0F, 1F,
                "Illusion Lantern form: execute damage is the larger of 5 x soul level and this fraction of max health.");
        illusionFlameRatio = config.getFloat("IllusionFlameRatio", CATEGORY, 1.5F, 0F, 20F,
                "Illusion Lantern soul flame damage, as a multiplier of attack damage.");

        nebulaBlinkRatio = config.getFloat("NebulaBlinkRatio", CATEGORY, 2.0F, 0F, 20F,
                "Eye of the Nebula blink strike damage, as a multiplier of attack damage.");
        nebulaPathRatio = config.getFloat("NebulaPathRatio", CATEGORY, 0.8F, 0F, 20F,
                "Eye of the Nebula max-level path damage, as a multiplier of attack damage.");

        voidParryRatio = config.getFloat("VoidParryRatio", CATEGORY, 1.5F, 0F, 20F,
                "Void Pearl parry: damage to nearby enemies, as a multiplier of attack damage.");
        voidParryReflect = config.getFloat("VoidParryReflect", CATEGORY, 0.5F, 0F, 10F,
                "Void Pearl parry: fraction of the parried damage reflected to the attacker as magic damage.");
        voidPurgeRatio = config.getFloat("VoidPurgeRatio", CATEGORY, 0.15F, 0F, 5F,
                "Void Pearl form at max level: extra damage per purged buff level, as a fraction of the hit.");
        primevalVoidParryNoCooldown = config.getBoolean("PrimevalVoidParryNoCooldown", CATEGORY, true,
                "Primeval resonance in Void Pearl form: raising and landing a parry triggers no cooldown.");

        cubeVoidRatio = config.getFloat("CubeVoidRatio", CATEGORY, 0.5F, 0F, 1F,
                "Non-Euclidean form: fraction of each melee hit dealt as void damage "
                        + "(ignores armor, Protection and Resistance; credited to the wielder).");

        aoeSparesAllies = config.getBoolean("AoeSparesAllies", CATEGORY, true,
                "Area skills (parry, flame breath, lava burst, freeze burst, light burst, nebula path, sweeps) "
                        + "skip the wielder's own tamed creatures, teammates, and other players unless PvP is on.");
    }
}
