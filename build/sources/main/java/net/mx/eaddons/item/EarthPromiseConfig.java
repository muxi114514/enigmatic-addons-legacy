package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

public class EarthPromiseConfig {
    /** Mining speed bonus, percentage (e.g. 20 = +20%). */
    public static int breakSpeedPercent = 20;
    /** Armor points. */
    public static double armorBonus = 5.0;
    /** Armor toughness. */
    public static double toughnessBonus = 2.0;
    /** Cooldown after negating damage, in ticks (50 seconds = 1000). */
    public static int cooldownTicks = 1000;
    /** Trigger threshold: damage >= this % of current health to negate (e.g. 80). */
    public static int abilityTriggerPercent = 80;
    /** First Curse correction: reduce damage by this % when wearing Cursed Ring (e.g. 25). */
    public static int firstCurseResistancePercent = 25;
    /** Fortune level bonus. */
    public static int fortuneBonus = 2;
    /** When Lost Engine (EnigmaticLegacy) is equipped, cooldowns are multiplied by this (e.g. 0.75 = 25%% reduction). */
    public static float lostEngineCooldownFactor = 0.75F;

    public static void init(Configuration config) {

        breakSpeedPercent = config.getInt("BreakSpeed", "PromiseOfTheEarth", 20, 0, 1000,
                "Mining speed boost (percentage).");

        armorBonus = config.getFloat("Armor", "PromiseOfTheEarth", 5.0F, 0, 256,
                "Armor points provided by the ring.");

        toughnessBonus = config.getFloat("Toughness", "PromiseOfTheEarth", 2.0F, 0, 256,
                "Armor toughness provided by the ring.");

        cooldownTicks = config.getInt("CooldownTicks", "PromiseOfTheEarth", 1000, 0, 32768,
                "Cooldown after negating a lethal hit (ticks). 20 ticks = 1 second. 1000 = 50 seconds.");

        abilityTriggerPercent = config.getInt("AbilityTriggerPercent", "PromiseOfTheEarth", 80, 1, 100,
                "When damage >= this % of current health, the hit is negated (and cooldown starts).");

        firstCurseResistancePercent = config.getInt("FirstCurseResistancePercent", "PromiseOfTheEarth", 25, 0, 100,
                "When wearing Cursed Ring (EnigmaticLegacy), reduce incoming damage by this % (First Curse correction).");

        fortuneBonus = config.getInt("FortuneBonus", "PromiseOfTheEarth", 2, 0, 10,
                "Fortune level bonus when mining.");

        lostEngineCooldownFactor = config.getFloat("LostEngineCooldownFactor", "PromiseOfTheEarth", 0.75F, 0.01F, 1.0F,
                "When Lost Engine (EnigmaticLegacy) is equipped, Earth Promise and Hell Blade Charm cooldowns are multiplied by this. 0.75 = 25% reduction.");

    }

    public static float getBreakSpeedMultiplier() {
        return breakSpeedPercent / 100.0F;
    }

    public static float getFirstCurseResistanceMultiplier() {
        return firstCurseResistancePercent / 100.0F;
    }
}
