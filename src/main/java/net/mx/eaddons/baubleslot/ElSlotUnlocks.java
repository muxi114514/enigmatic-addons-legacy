package net.mx.eaddons.baubleslot;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import keletu.enigmaticlegacy.util.compat.ModCompat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 天体果实（+1 戒指槽）与灵液瓶（+1 项链槽）的开槽逻辑，原版 Baubles 下改走 BaubleVault。
 * <p>次数与 EL 共用同一组持久计数（ConsumedAstralFruit / ConsumedIchorBottle），上限读 EL 配置，
 * 所以别的来源（如神秘佳肴的神圣果派）先开过，再吃果实也不会重复开槽。
 * 附属模组可调用 {@link #grantAstralFruitSlot} / {@link #grantIchorBottleSlot} / {@link #unlockExtra}。
 */
public final class ElSlotUnlocks {

    private static final Logger LOG = LogManager.getLogger("eaddons");
    private static final String BAUBLE_VAULT = "unlockablebauble";

    private ElSlotUnlocks() {
    }

    /** EL 自己能处理（装了 BaublesEX） */
    public static boolean handledByEnigmaticLegacy() {
        return ModCompat.COMPAT_BAUBLES_EX;
    }

    /** 当前环境下吃果实 / 灵液瓶能否真的开槽 */
    public static boolean slotUnlockAvailable() {
        return handledByEnigmaticLegacy() || Loader.isModLoaded(BAUBLE_VAULT);
    }

    /** 按天体果实的规则开一个戒指槽，返回是否开成 */
    public static boolean grantAstralFruitSlot(EntityPlayerMP player) {
        return grant(player, "ConsumedAstralFruit", EnigmaticConfigs.maxAstralFruitSlotUnlocks,
                SlotKind.RING, BaubleSlotConfig.astralFruitRingSlot);
    }

    /** 按灵液瓶的规则开一个项链槽，返回是否开成 */
    public static boolean grantIchorBottleSlot(EntityPlayerMP player) {
        return grant(player, "ConsumedIchorBottle", EnigmaticConfigs.maxIchorBottleSlotUnlocks,
                SlotKind.AMULET, BaubleSlotConfig.ichorBottleAmuletSlot);
    }

    /** 直接解锁 BaubleVault 的第 index 个额外槽；BaubleVault 未加载时返回 false */
    public static boolean unlockExtra(EntityPlayerMP player, SlotKind kind, int index) {
        if (!Loader.isModLoaded(BAUBLE_VAULT)) {
            return false;
        }
        boolean unlocked = BaubleVaultBridge.unlockExtra(player, kind, index);
        if (!unlocked) {
            LOG.warn("BaubleVault did not unlock extra {} slot {} for {} (already unlocked, or no unlock rule like '{}:{}|level|2147483647')",
                    kind, index, player.getName(), kind, index);
        }
        return unlocked;
    }

    private static boolean grant(EntityPlayerMP player, String counterKey, int max, SlotKind kind, int firstIndex) {
        if (handledByEnigmaticLegacy()) {
            return false;
        }
        int used = SuperpositionHandler.getPersistentInteger(player, counterKey, 0);
        if (used >= max || !unlockExtra(player, kind, firstIndex + used)) {
            return false;
        }
        SuperpositionHandler.setPersistentInteger(player, counterKey, used + 1);
        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
