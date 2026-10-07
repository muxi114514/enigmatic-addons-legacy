package net.mx.eaddons.miningcharm;

import net.minecraftforge.common.config.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 猎宝者护符（enigmaticlegacy:mining_charm）的追加功能：任意饰品槽、永久夜视与连锁挖掘。 */
public final class MiningCharmConfig {
    private static final String CATEGORY = "MiningCharm";

    /** 饰品类型由挂饰改为万能，任何饰品格都能放。 */
    public static boolean anySlot = true;

    /** 佩戴即给夜视，去掉原版「地下、黑暗、主世界、不在水里」的限制。右键开关仍有效。 */
    public static boolean permanentNightVision = true;

    public static boolean veinMiningEnabled = true;
    /** 一次最多连带挖掉的方块数，不含起点。 */
    public static int veinMaxBlocks = 32;
    /** 连锁出来的掉落物与经验球集中到起点方块。 */
    public static boolean veinGatherDrops = true;
    /** 不参与连锁的方块注册名。 */
    public static Set<String> veinBlacklist = Collections.emptySet();

    private MiningCharmConfig() {
    }

    public static void init(Configuration config) {
        anySlot = config.getBoolean("AnySlot", CATEGORY, true,
                "Make the Charm of Treasure Hunter a trinket that fits any bauble slot instead of only charm slots. "
                        + "Enigmatic Legacy still allows only one to be worn. Requires game restart.");

        permanentNightVision =config.getBoolean("PermanentNightVision", CATEGORY, true,
                "Night Vision from the Charm of Treasure Hunter works everywhere while worn, instead of only "
                        + "underground in the dark. Right-clicking the charm still toggles it.");

        veinMiningEnabled = config.getBoolean("VeinMiningEnabled", CATEGORY, true,
                "While wearing the charm, holding the vein mining key when a block breaks also breaks the "
                        + "connected blocks of the same kind. Only the first block costs tool durability.");

        veinMaxBlocks = config.getInt("VeinMiningMaxBlocks", CATEGORY, 32, 1, 256,
                "Maximum number of extra blocks broken at once (the first block not included).");

        veinGatherDrops = config.getBoolean("VeinMiningGatherDrops", CATEGORY, true,
                "Move the drops and experience of the extra blocks to the first block.");

        String[] list = config.getStringList("VeinMiningBlacklist", CATEGORY, new String[0],
                "Registry names of blocks that never vein mine, e.g. minecraft:mob_spawner");
        veinBlacklist = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(list)));
    }
}
