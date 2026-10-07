package net.mx.eaddons.baubleslot;

import net.minecraftforge.common.config.Configuration;

/** 天体果实 / 灵液瓶在 BaubleVault 下解锁的额外槽位序号 */
public final class BaubleSlotConfig {

    private static final String CATEGORY = "BaubleVaultSlots";

    /** 天体果实解锁的第一个额外戒指槽（对应 BaubleVault 规则 RING:n），第 k 次解锁用 n+k */
    public static int astralFruitRingSlot = 4;
    /** 灵液瓶解锁的第一个额外项链槽（对应 BaubleVault 规则 AMULET:n） */
    public static int ichorBottleAmuletSlot = 1;

    private BaubleSlotConfig() {
    }

    public static void init(Configuration config) {
        astralFruitRingSlot = config.getInt("AstralFruitRingSlot", CATEGORY, 4, 0, 99,
                "First BaubleVault extra RING index unlocked by the Astral Fruit. BaubleVault must have a rule for it, "
                        + "e.g. 'RING:4|level|2147483647'. With MaxAstralFruitSlotUnlocks > 1, the following indices are used too.");
        ichorBottleAmuletSlot = config.getInt("IchorBottleAmuletSlot", CATEGORY, 1, 0, 99,
                "First BaubleVault extra AMULET index unlocked by the Ichor Bottle. BaubleVault must have a rule for it, "
                        + "e.g. 'AMULET:1|level|2147483647'.");
        config.setCategoryComment(CATEGORY,
                "Enigmatic Legacy's Astral Fruit and Ichor Bottle are always registered. With BaublesEX they use EL's own slot logic; "
                        + "with BaubleVault they unlock the slots below; with neither they only grant their effects.");
    }
}
