package net.mx.eaddons.item;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TotemOfMaliceConfig {
    /** Damage bonus to illagers, percentage (e.g. 150 = +150%). */
    public static int damageBonusPercent = 150;
    /** Damage reduction from illagers, percentage (e.g. 50 = 50% reduction). */
    public static int damageReductionPercent = 50;
    /** Base durability (uses) before Unbreaking. */
    public static int baseDurability = 1;
    /** Death burst damage = holder max HP * this (e.g. 1.5 = 1.5x max HP). */
    public static float deathBurstMultiplier = 1.5F;
    /** Each curse enchant adds this % to death burst (e.g. 50 = +50% per curse). */
    public static int curseBonusPercent = 50;
    /** AOE radius (blocks) for death burst. */
    public static float aoeRadius = 4.0F;
    /** Repair item registry name (e.g. enigmaticlegacy:evil_essence). */
    public static String repairItemRegistryName = "enigmaticlegacy:evil_essence";
    /** Comma-separated illager entity IDs (e.g. minecraft:evoker,minecraft:vindicator). Empty = use vanilla AbstractIllager. */
    public static String illagerEntityIds = "minecraft:evoker,minecraft:vindicator,minecraft:illusion_illager";

    private static Set<ResourceLocation> illagerIdsParsed = null;

    public static void init(Configuration config) {

        damageBonusPercent = config.getInt("DamageBonusPercent", "TotemOfMalice", 150, 0, 1000,
                "Damage bonus to illagers (percentage). 150 = +150%.");

        damageReductionPercent = config.getInt("DamageReductionPercent", "TotemOfMalice", 50, 0, 100,
                "Damage reduction from illagers (percentage). 50 = 50% less damage taken.");

        baseDurability = config.getInt("BaseDurability", "TotemOfMalice", 1, 1, 100,
                "Base durability (uses). Each Unbreaking level adds 1 to max.");

        deathBurstMultiplier = config.getFloat("DeathBurstMultiplier", "TotemOfMalice", 1.5F, 0.1F, 100.0F,
                "Death burst damage = holder max HP × this value.");

        curseBonusPercent = config.getInt("CurseBonusPercent", "TotemOfMalice", 50, 0, 500,
                "Each curse enchant on the totem adds this % to death burst damage.");

        aoeRadius = config.getFloat("AoeRadius", "TotemOfMalice", 4.0F, 0.5F, 32.0F,
                "Radius (blocks) for death burst AOE damage.");

        repairItemRegistryName = config.getString("RepairItemRegistryName", "TotemOfMalice", "enigmaticlegacy:evil_essence",
                "Registry name of item used to repair the totem (e.g. enigmaticlegacy:evil_essence).");

        illagerEntityIds = config.getString("IllagerEntityIds", "TotemOfMalice",
                "minecraft:evoker,minecraft:vindicator,minecraft:illusion_illager",
                "Comma-separated entity registry names treated as illagers. Empty = use vanilla AbstractIllager only.");

        illagerIdsParsed = null;
    }

    /** Parsed set of illager entity IDs from config. */
    public static Set<ResourceLocation> getIllagerEntityIds() {
        if (illagerIdsParsed == null) {
            Set<ResourceLocation> set = new HashSet<>();
            String s = illagerEntityIds;
            if (s != null && !s.trim().isEmpty()) {
                for (String part : s.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        if (trimmed.indexOf(':') < 0) {
                            set.add(new ResourceLocation("minecraft", trimmed));
                        } else {
                            String[] sp = trimmed.split(":", 2);
                            set.add(new ResourceLocation(sp[0], sp[1]));
                        }
                    }
                }
            }
            illagerIdsParsed = Collections.unmodifiableSet(set);
        }
        return illagerIdsParsed;
    }
}
