package net.mx.eaddons.item;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ForgerGemConfig {
    public static boolean strictUnbreakableForge = true;
    public static final List<ResourceLocation> blacklist = new ArrayList<>();

    private static final String[] DEFAULT_BLACKLIST = new String[]{};

    public static void init(Configuration config) {

        strictUnbreakableForge = config.getBoolean("StrictUnbreakableForge", "ForgerGem", true,
                "When true, only tools, swords, and armor can be made unbreakable via Forger's Gem. "
                        + "When false, any damageable item can be made unbreakable.");

        String[] list = config.getStringList("UnbreakableBlacklist", "ForgerGem", DEFAULT_BLACKLIST,
                "Items that can never be made unbreakable. Format: modid:itemname. "
                        + "Example: minecraft:elytra");

        blacklist.clear();
        for (String entry : list) {
            if (!entry.isEmpty()) {
                blacklist.add(new ResourceLocation(entry));
            }
        }

    }

    public static boolean isBlacklisted(ResourceLocation itemId) {
        return blacklist.contains(itemId);
    }
}
