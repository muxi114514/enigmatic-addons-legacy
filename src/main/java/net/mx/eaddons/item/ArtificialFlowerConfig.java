package net.mx.eaddons.item;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtificialFlowerConfig {
    /** 邪恶精髓洗练与术质核心自选的属性上限（%）。 */
    public static int randomAttributeMaxModifier = 16;
    /** 青金石洗练的属性上限（%）。 */
    public static int lapisAttributeMaxModifier = 8;
    /** 邪恶精髓每次洗练（属性或常驻效果）的消耗。 */
    public static int evilEssenceCost = 1;
    /** 石英洗练常驻效果时只从这份名单里抽；邪恶精髓则抽名单以外的（不可变集合，读写跨线程安全）。 */
    private static volatile Set<ResourceLocation> basicProvidedEffects = Collections.emptySet();
    /** 只禁免疫槽：这些效果仍可被抽进常驻槽，但不能被免疫（机制标记、Boss 惩罚等）。 */
    private static volatile Set<ResourceLocation> immunityBlacklist = Collections.emptySet();
    public static int randomInstantaneousEffectModifier = 80;
    /** 佩戴神秘遗物「非欧立方」时，为石英花提供的增益额外增加的等级数（0 = 关闭该联动）。 */
    public static int theCubeBonusLevel = 1;
    public static final List<String> attributeBlacklist = new ArrayList<>();
    public static final List<ResourceLocation> effectBlacklist = new ArrayList<>();
    /** 只禁止用术质核心自选、随机洗练仍可能出现的属性 / 效果（不可变集合，读写跨线程安全）。 */
    private static volatile Set<String> choiceAttributeBlacklist = Collections.emptySet();
    private static volatile Set<String> choiceEffectBlacklist = Collections.emptySet();

    /** 本模组的自定义属性：基值 0 的抽到也是白抽，下落速度抽到正值反成减益，默认全部排除 */
    private static final String[] DEFAULT_ATTRIBUTE_BLACKLIST = new String[]{
            "eaddons.lifesteal", "eaddons.projectileDeflect", "eaddons.etheriumShield", "eaddons.miningSpeed",
            "eaddons.fallSpeed", "eaddons.jumpBoost", "eaddons.critDamage", "eaddons.fallImmunity"
    };

    private static final String[] DEFAULT_EFFECT_BLACKLIST = new String[]{
            "enigmaticlegacy:blazing_strength"
    };

    private static final String[] DEFAULT_BASIC_PROVIDED_EFFECTS = new String[]{
            "minecraft:speed", "minecraft:haste", "minecraft:jump_boost", "minecraft:night_vision",
            "minecraft:water_breathing", "minecraft:fire_resistance", "minecraft:luck"
    };

    /** 巧克力任务的 Boss 机制与地牢标记、Boss Rush 状态、moremod 石像守卫规则、PotionCore 疾病 */
    private static final String[] DEFAULT_IMMUNITY_BLACKLIST = new String[]{
            "potioncore:potion_sickness", "chocotweak:stun", "chocotweak:wound", "chocotweak:magic_vulnerability",
            "chocotweak:exhaustion", "chocotweak:phantom_curse", "chocotweak:crimson_rust", "chocotweak:aigua_freeze",
            "chocotweak:bone_crush", "chocotweak:collapse", "chocotweak:cracked_horn", "chocotweak:earth_shatter",
            "chocotweak:deadly_venom", "chocotweak:rush_heal_30", "chocotweak:rush_heal_50", "chocotweak:rush_heal_80",
            "chocotweak:rush_no_teleport", "moremod:moon_affliction", "moremod:please_look_at_me",
            "moremod:subject_dissolution", "chocolatequest:mine_prevention", "chocotweak:dungeon_explorer"
    };

    public static void init(Configuration config) {

        randomAttributeMaxModifier = config.getInt("RandomAttributeMaxModifier", "ArtificialFlower", 16, 0, 100,
                "The max modifier of the Magic Quartz Flower when rerolling with Evil Essence, and the value a "
                        + "Spellcore pick sets. Measures in percentage.");

        lapisAttributeMaxModifier = config.getInt("LapisAttributeMaxModifier", "ArtificialFlower", 8, 0, 100,
                "The max modifier of the Magic Quartz Flower when rerolling with lapis lazuli. Measures in percentage.");

        evilEssenceCost = config.getInt("EvilEssenceCost", "ArtificialFlower", 1, 1, 64,
                "Evil Essence consumed per reroll (an attribute or the provided effect). It uses the best lapis tier's "
                        + "distribution and cannot reroll the immunity effect.");

        immunityBlacklist = toLocations(config.getStringList("ImmunityBlackList", "ArtificialFlower",
                DEFAULT_IMMUNITY_BLACKLIST, "Effects the immunity slot can never roll or pick, and never block even on "
                        + "an older flower that already has one (that entry is removed). They can still be rolled as the "
                        + "provided effect unless also in EffectBlackList. Format: modid:effectname"));

        basicProvidedEffects = toLocations(config.getStringList("BasicProvidedEffects", "ArtificialFlower",
                DEFAULT_BASIC_PROVIDED_EFFECTS, "Effects the provided slot can roll with quartz (only these). Evil "
                        + "Essence rolls every other effect of the usual pool instead. The immunity slot is unaffected. "
                        + "Format: modid:effectname"));

        randomInstantaneousEffectModifier = config.getInt("RandomInstantaneousEffectModifier", "ArtificialFlower", 80, 0, 100,
                "The modifier of the instantaneous effect provided by Magic Quartz Flower. Measures in percentage.");

        theCubeBonusLevel = config.getInt("TheCubeBonusLevel", "ArtificialFlower", 1, 0, 3,
                "Extra effect levels granted to the Magic Quartz Flower while wearing Enigmatic Legacy's The Cube. 0 disables this synergy.");

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

        choiceAttributeBlacklist = toSet(config.getStringList("ChoiceAttributeBlackList", "ArtificialFlower",
                new String[0], "Attributes that cannot be picked with a Spellcore in the flower GUI (they can "
                        + "still be rolled randomly). Format: generic.maxHealth"));
        choiceEffectBlacklist = toSet(config.getStringList("ChoiceEffectBlackList", "ArtificialFlower",
                new String[0], "Potion effects that cannot be picked with a Spellcore in the flower GUI, as the "
                        + "provided or the immunity effect (they can still be rolled randomly). Format: modid:effectname"));
    }

    private static Set<ResourceLocation> toLocations(String[] entries) {
        Set<ResourceLocation> set = new HashSet<>();
        for (String entry : entries) {
            if (entry != null && !entry.trim().isEmpty()) {
                set.add(new ResourceLocation(entry.trim()));
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public static boolean isBasicProvidedEffect(ResourceLocation effectId) {
        return effectId != null && basicProvidedEffects.contains(effectId);
    }

    public static boolean isImmunityBlacklisted(ResourceLocation effectId) {
        return effectId != null && immunityBlacklist.contains(effectId);
    }

    public static Set<ResourceLocation> basicProvidedEffects() {
        return basicProvidedEffects;
    }

    private static Set<String> toSet(String[] entries) {
        Set<String> set = new HashSet<>(Arrays.asList(entries));
        set.remove("");
        return Collections.unmodifiableSet(set);
    }

    public static boolean isAttributeBlacklisted(String attributeName) {
        return attributeBlacklist.contains(attributeName);
    }

    public static boolean isEffectBlacklisted(ResourceLocation effectId) {
        return effectBlacklist.contains(effectId);
    }

    public static boolean isChoiceAttributeBlacklisted(String attributeName) {
        return choiceAttributeBlacklist.contains(attributeName);
    }

    public static boolean isChoiceEffectBlacklisted(ResourceLocation effectId) {
        return choiceEffectBlacklist.contains(effectId.toString());
    }
}
