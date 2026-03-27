package net.mx.eaddons.item;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ArtificialFlowerConfig {
    public static int randomAttributeMaxModifier = 16;
    public static int randomInstantaneousEffectModifier = 80;
    public static final List<String> attributeBlacklist = new ArrayList<>();
    public static final List<ResourceLocation> effectBlacklist = new ArrayList<>();

    private static final String[] DEFAULT_ATTRIBUTE_BLACKLIST = new String[]{};

    private static final String[] DEFAULT_EFFECT_BLACKLIST = new String[]{
            "enigmaticlegacy:blazing_strength"
    };

    public static void init(Configuration config) {

        randomAttributeMaxModifier = config.getInt("RandomAttributeMaxModifier", "ArtificialFlower", 16, 0, 100,
                "The max modifier of the Magic Quartz Flower. Measures in percentage.");

        randomInstantaneousEffectModifier = config.getInt("RandomInstantaneousEffectModifier", "ArtificialFlower", 80, 0, 100,
                "The modifier of the instantaneous effect provided by Magic Quartz Flower. Measures in percentage.");

        String[] attrList = config.getStringList("AttributeBlackList", "ArtificialFlower", DEFAULT_ATTRIBUTE_BLACKLIST,
                "List of attribute names that will never appear on the Magic Quartz Flower. "
                        + "Format: generic.armor, generic.maxHealth, etc. Requires game restart.");

        attributeBlacklist.clear();
        for (String entry : attrList) {
            if (!entry.isEmpty()) {
                attributeBlacklist.add(entry);
            }
        }

        String[] effectList = config.getStringList("EffectBlackList", "ArtificialFlower", DEFAULT_EFFECT_BLACKLIST,
                "List of potion effects that will never appear on the Magic Quartz Flower. "
                        + "Format: modid:effectname. Requires game restart.");

        effectBlacklist.clear();
        for (String entry : effectList) {
            if (!entry.isEmpty()) {
                effectBlacklist.add(new ResourceLocation(entry));
            }
        }

    }

    public static boolean isAttributeBlacklisted(String attributeName) {
        return attributeBlacklist.contains(attributeName);
    }

    public static boolean isEffectBlacklisted(ResourceLocation effectId) {
        return effectBlacklist.contains(effectId);
    }
}
