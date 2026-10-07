package net.mx.eaddons.elplus;

import net.minecraftforge.common.config.Configuration;

/** 从 EL+（神秘遗物 1.21 续作）补回的材料与物品的获取数值 */
public final class ElPlusConfig {

    private static final String CATEGORY = "EnigmaticLegacyPlusItems";

    /** 七咒佩戴者击杀烈焰人掉狱火余烬的概率与每级抢夺加成 */
    public static float cinderBlazeChance = 0.3F;
    public static float cinderBlazeLootingBonus = 0.05F;
    public static int cinderBlazeMin = 1;
    public static int cinderBlazeMax = 3;
    /** 下界要塞箱子里狱火余烬条目与空条目的权重（默认约 6.7%） */
    public static int cinderNetherChestWeight = 1;
    public static int cinderNetherChestEmptyWeight = 14;

    /** 玩家击杀潜影贝掉以太粒的数量区间（另加 0~抢夺等级） */
    public static int nuggetShulkerMin = 1;
    public static int nuggetShulkerMax = 2;

    /** 玩家击杀敌对生物掉大地之心碎片的概率与每级抢夺加成 */
    public static float fragmentChance = 0.01F;
    public static float fragmentLootingBonus = 0.005F;

    /** 行刑者之斧的斩首基础概率与每级抢夺加成 */
    public static float beheadChance = 0.10F;
    public static float beheadLootingBonus = 0.05F;

    private ElPlusConfig() {
    }

    public static void init(Configuration config) {
        cinderBlazeChance = config.getFloat("CinderBlazeChance", CATEGORY, 0.3F, 0, 1,
                "Chance for a Blaze killed by a Seven Curses bearer to drop Infernal Cinder.");
        cinderBlazeLootingBonus = config.getFloat("CinderBlazeLootingBonus", CATEGORY, 0.05F, 0, 1,
                "Extra Infernal Cinder drop chance per Looting level.");
        cinderBlazeMin = config.getInt("CinderBlazeMin", CATEGORY, 1, 1, 64, "Minimum Infernal Cinder dropped by a Blaze.");
        cinderBlazeMax = config.getInt("CinderBlazeMax", CATEGORY, 3, 1, 64, "Maximum Infernal Cinder dropped by a Blaze.");
        cinderNetherChestWeight = config.getInt("CinderNetherChestWeight", CATEGORY, 1, 0, 1000,
                "Weight of Infernal Cinder in Nether Fortress chests (0 disables).");
        cinderNetherChestEmptyWeight = config.getInt("CinderNetherChestEmptyWeight", CATEGORY, 14, 0, 1000,
                "Weight of the empty entry next to Infernal Cinder in Nether Fortress chests.");

        nuggetShulkerMin = config.getInt("NuggetShulkerMin", CATEGORY, 1, 0, 64,
                "Minimum Etherium Nuggets dropped by a Shulker killed by a player.");
        nuggetShulkerMax = config.getInt("NuggetShulkerMax", CATEGORY, 2, 0, 64,
                "Maximum Etherium Nuggets dropped by a Shulker (Looting adds 0..level more).");

        fragmentChance = config.getFloat("EarthFragmentChance", CATEGORY, 0.01F, 0, 1,
                "Chance for a hostile mob killed by a player to drop a Fragment of the Earth.");
        fragmentLootingBonus = config.getFloat("EarthFragmentLootingBonus", CATEGORY, 0.005F, 0, 1,
                "Extra Fragment of the Earth drop chance per Looting level.");

        beheadChance = config.getFloat("BeheadChance", CATEGORY, 0.10F, 0, 1,
                "Base beheading chance of the Axe of Executioner.");
        beheadLootingBonus = config.getFloat("BeheadLootingBonus", CATEGORY, 0.05F, 0, 1,
                "Extra beheading chance per Looting level.");

        config.setCategoryComment(CATEGORY,
                "Items backported from Enigmatic Legacy+ (1.21): Infernal Cinder, Etherium Nugget, Fragment of the Earth, Axe of Executioner, etc.");
    }
}
