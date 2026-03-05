package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class DragonBowConfig {
    public static int maxPotionAmount = 4;
    public static int ownerResistance = 60;

    public static void init(File configDir) {
        Configuration config = new Configuration(new File(configDir, "jmheaven_dragon_bow.cfg"));
        config.load();

        maxPotionAmount = config.getInt("MaxPotionAmount", "DragonBreathBow", 4, 1, 10,
                "The max amount of potion effects you can apply on the arrow.");

        ownerResistance = config.getInt("OwnerResistance", "DragonBreathBow", 60, 0, 100,
                "The damage resistance (percentage) to your own dragon breath damage.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static float getOwnerResistanceMultiplier() {
        return 1.0F - ownerResistance / 100.0F;
    }
}
