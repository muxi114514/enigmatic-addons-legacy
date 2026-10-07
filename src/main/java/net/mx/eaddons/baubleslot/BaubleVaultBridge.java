package net.mx.eaddons.baubleslot;

import com.unlockablebauble.accessorybox.unlock.SlotUnlockAPI;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 唯一直接引用 BaubleVault 的类。调用方必须先确认 BaubleVault 已加载，
 * 否则加载本类会 NoClassDefFoundError。
 */
final class BaubleVaultBridge {

    private BaubleVaultBridge() {
    }

    /** 解锁某类型的第 index 个额外槽（从 0 起）；没有对应规则或已解锁时返回 false */
    static boolean unlockExtra(EntityPlayer player, SlotKind kind, int index) {
        switch (kind) {
            case RING:
                return SlotUnlockAPI.unlockExtraRing(player, index);
            case AMULET:
                return SlotUnlockAPI.unlockExtraAmulet(player, index);
            case CHARM:
                return SlotUnlockAPI.unlockExtraCharm(player, index);
            case TRINKET:
                return SlotUnlockAPI.unlockExtraTrinket(player, index);
            default:
                return false;
        }
    }
}
