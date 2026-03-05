package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/** Config for Emblem of Adventurer and Insignia of Despair. */
public class EmblemAdventurerConfig {
    // Emblem of Adventurer
    public static double adventurerAttackDamage = 2.0;
    public static double adventurerAttackSpeedPercent = 10.0;
    // Insignia of Despair
    public static double despairAttackDamage = 4.0;
    public static double despairAttackSpeedPercent = 16.0;
    public static double despairMovementSpeedPercent = 5.0;
    public static double despairKnockbackResistancePercent = -5.0;

    public static void init(File configDir) {
        Configuration config = new Configuration(new File(configDir, "jmheaven_emblem_adventurer.cfg"));
        config.load();

        adventurerAttackDamage = config.getFloat("AdventurerAttackDamage", "EmblemOfAdventurer", 2.0F, 0, 256,
                "Attack damage bonus when wearing Emblem of Adventurer.");
        adventurerAttackSpeedPercent = config.getFloat("AdventurerAttackSpeedPercent", "EmblemOfAdventurer", 10.0F, 0, 100,
                "Attack speed bonus (percentage, e.g. 10 = +10%%) when wearing Emblem of Adventurer.");

        despairAttackDamage = config.getFloat("DespairAttackDamage", "InsigniaOfDespair", 4.0F, 0, 256,
                "Attack damage bonus when wearing Insignia of Despair.");
        despairAttackSpeedPercent = config.getFloat("DespairAttackSpeedPercent", "InsigniaOfDespair", 16.0F, 0, 100,
                "Attack speed bonus (percentage) when wearing Insignia of Despair.");
        despairMovementSpeedPercent = config.getFloat("DespairMovementSpeedPercent", "InsigniaOfDespair", 5.0F, 0, 100,
                "Movement speed bonus (percentage) when wearing Insignia of Despair.");
        despairKnockbackResistancePercent = config.getFloat("DespairKnockbackResistancePercent", "InsigniaOfDespair", -5.0F, -100, 0,
                "Knockback resistance change (percentage, negative = less resistance) when wearing Insignia of Despair.");

        if (config.hasChanged()) config.save();
    }
}
