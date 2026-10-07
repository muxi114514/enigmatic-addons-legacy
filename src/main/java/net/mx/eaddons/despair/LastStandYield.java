package net.mx.eaddons.despair;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import keletu.enigmaticlegacy.event.EnigmaticEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.mx.eaddons.item.ItemTotemOfMalice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 「接下来一定能救下玩家」的其它免死效果登记表。绝境触发前逐条询问，任一条成立就让位，
 * 免得抢在恶意图腾、非欧立方复活前面白白进入冷却。附属模组可自行 {@link #register}。
 *
 * <p>原版图腾与虚空调谐跟绝境挂在同一个检查里，天然排在前面，不用登记；
 * 虚空珍珠、无止之言这类概率免死无法预知结果，绝境会先于它们触发。
 */
public final class LastStandYield {
    private static final Logger LOG = LogManager.getLogger("eaddons");
    private static final List<Predicate<EntityPlayer>> RULES = new CopyOnWriteArrayList<>();

    private LastStandYield() {
    }

    public static void register(Predicate<EntityPlayer> rule) {
        RULES.add(rule);
    }

    /** 本模组的恶意图腾与神秘遗物的非欧立方。 */
    public static void registerDefaults() {
        register(player -> ItemTotemOfMalice.getRemainingUses(ItemTotemOfMalice.getTotemStack(player)) > 0);
        register(LastStandYield::cubeWillSave);
    }

    static boolean anyWillSave(EntityPlayer player) {
        for (Predicate<EntityPlayer> rule : RULES) {
            try {
                if (rule.test(player)) {
                    return true;
                }
            } catch (RuntimeException e) {
                LOG.warn("[LastStand] A yield rule failed and was skipped", e);
            }
        }
        return false;
    }

    /** 与 EL 死亡事件里的两处对应：复活冷却就绪；或受伤前生命 ≥ 1.5（装 First Aid 时玩家不会记录这一项）。 */
    private static boolean cubeWillSave(EntityPlayer player) {
        Item cube = EnigmaticLegacy.the_cube;
        if (cube == null || BaublesApi.isBaubleEquipped(player, cube) == -1) {
            return false;
        }
        if (IForbiddenConsumed.get(player).getSpellstoneCooldown() == 0) {
            return true;
        }
        Float lastHealth = EnigmaticEvents.LAST_HEALTH.get(player);
        return lastHealth != null && lastHealth >= 1.5F;
    }
}
