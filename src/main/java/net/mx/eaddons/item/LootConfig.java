package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 宝箱战利品权重配置。权重为同池内相对值：
 * 某物品出现概率 = 该物品权重 / 池内权重总和；"空"条目权重越高，整体出货率越低。
 */
public class LootConfig {
    private static final String CATEGORY = "ChestLoot";

    /** 主世界宝箱池（地牢/废弃矿井/沙漠神殿/丛林神庙/要塞走廊/末地城）。 */
    public static int overworldFlowerWeight = 10;
    public static int overworldBagWeight = 20;
    public static int overworldForgerGemWeight = 35;
    public static int overworldEmptyWeight = 35;

    /** 下界要塞宝箱池。 */
    public static int netherIchorWeight = 75;
    public static int netherHellBladeWeight = 4;
    public static int netherEmptyWeight = 21;

    /**
     * 术质共鸣者体系的掉落倍率（百分比）。
     * <p>基准概率按 EL 自己术石在各箱子里的实际出现率逐表设定（见 LootHandler），
     * 这里只做整体缩放：100 = 与术石同概率，0 = 关闭。
     */
    public static int spellstoneLootScale = 100;

    public static void init(Configuration config) {
        overworldFlowerWeight = config.getInt("OverworldFlowerWeight", CATEGORY, 10, 0, 1000,
                "Weight of Magic Quartz Flower in overworld chest pool (dungeon/mineshaft/"
                        + "desert pyramid/jungle temple/stronghold corridor/end city).");
        overworldBagWeight = config.getInt("OverworldBagWeight", CATEGORY, 20, 0, 1000,
                "Weight of Antique Bag in overworld chest pool.");
        overworldForgerGemWeight = config.getInt("OverworldForgerGemWeight", CATEGORY, 35, 0, 1000,
                "Weight of Forger's Gem in overworld chest pool.");
        overworldEmptyWeight = config.getInt("OverworldEmptyWeight", CATEGORY, 35, 0, 1000,
                "Weight of the empty entry in overworld chest pool (higher = lower overall drop rate).");

        netherIchorWeight = config.getInt("NetherIchorWeight", CATEGORY, 75, 0, 1000,
                "Weight of Ichor Droplet (1-3) in nether fortress chest pool.");
        netherHellBladeWeight = config.getInt("NetherHellBladeWeight", CATEGORY, 4, 0, 1000,
                "Weight of Charm of Hell Blade in nether fortress chest pool.");
        spellstoneLootScale = config.getInt("SpellstoneLootScale", CATEGORY, 100, 0, 1000,
                "Scale for spellstone-related chest loot, in percent. 100 means the same chance as "
                        + "Enigmatic Legacy's own spellstones in that chest. 0 disables it.");

        netherEmptyWeight = config.getInt("NetherEmptyWeight", CATEGORY, 21, 0, 1000,
                "Weight of the empty entry in nether fortress chest pool (higher = lower overall drop rate).");
    }
}
