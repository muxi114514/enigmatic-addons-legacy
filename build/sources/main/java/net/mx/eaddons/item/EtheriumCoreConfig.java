package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class EtheriumCoreConfig {
    /** Cooldown after activating shield, in ticks (40s = 800). */
    public static int cooldownTicks = 800;
    /** Shield duration when activated, in ticks (25s = 500). */
    public static int shieldDurationTicks = 500;
    /** When Lost Engine is equipped, cooldown is multiplied by this (e.g. 0.75 = 25% reduction). */
    public static float lostEngineCooldownFactor = 0.75F;
    /** Flat armor bonus. */
    public static double armorBonus = 12.0;
    /** Flat armor toughness bonus. */
    public static double armorToughnessBonus = 10.0;
    /** Armor multiplier (e.g. 0.2 = +20%). */
    public static float armorMultiplier = 0.2F;
    /** Armor toughness multiplier (e.g. 0.4 = +40%). */
    public static float armorToughnessMultiplier = 0.4F;
    /** Knockback resistance (0–1, e.g. 0.5 = 50%). */
    public static float knockbackResistance = 0.5F;
    /** Damage-to-attack conversion ratio (e.g. 0.4 = 40% of taken damage). */
    public static float damageConversion = 0.4F;
    /** Cap for stored damage bonus. */
    public static float damageConversionCap = 25.0F;
    /** When wearing full Etherium set + Core, shield threshold = base (40%) * this (1.5 = 60%). */
    public static float shieldThresholdMultiplier = 1.5F;

    public static void init(File configDir) {
        Configuration config = new Configuration(new File(configDir, "jmheaven_etherium_core.cfg"));
        config.load();

        cooldownTicks = config.getInt("CooldownTicks", "EtheriumCore", 800, 1, 32768,
                "Cooldown after activating Etherium Shield (ticks). 20 = 1 second. 800 = 40 seconds.");
        shieldDurationTicks = config.getInt("ShieldDurationTicks", "EtheriumCore", 500, 1, 32768,
                "Etherium Shield duration when activated (ticks). 500 = 25 seconds.");
        lostEngineCooldownFactor = config.getFloat("LostEngineCooldownFactor", "EtheriumCore", 0.75F, 0.01F, 1.0F,
                "When Lost Engine is equipped, cooldown is multiplied by this. 0.75 = 25% reduction.");
        armorBonus = config.getFloat("ArmorBonus", "EtheriumCore", 12.0F, 0, 256,
                "Flat armor points.");
        armorToughnessBonus = config.getFloat("ArmorToughnessBonus", "EtheriumCore", 10.0F, 0, 256,
                "Flat armor toughness.");
        armorMultiplier = config.getFloat("ArmorMultiplier", "EtheriumCore", 0.2F, 0, 10,
                "Armor multiplier (0.2 = +20%).");
        armorToughnessMultiplier = config.getFloat("ArmorToughnessMultiplier", "EtheriumCore", 0.4F, 0, 10,
                "Armor toughness multiplier (0.4 = +40%).");
        knockbackResistance = config.getFloat("KnockbackResistance", "EtheriumCore", 0.5F, 0, 1,
                "Knockback resistance (0.5 = 50%).");
        damageConversion = config.getFloat("DamageConversion", "EtheriumCore", 0.4F, 0, 1,
                "Fraction of taken damage converted to next attack bonus (0.4 = 40%).");
        damageConversionCap = config.getFloat("DamageConversionCap", "EtheriumCore", 25.0F, 0, 1000,
                "Maximum stored damage bonus.");
        shieldThresholdMultiplier = config.getFloat("ShieldThresholdMultiplier", "EtheriumCore", 1.5F, 1, 3,
                "When wearing full Etherium set + Core, shield threshold = 40% * this (1.5 = 60%).");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
