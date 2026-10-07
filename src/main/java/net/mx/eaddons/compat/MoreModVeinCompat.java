package net.mx.eaddons.compat;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

/**
 * moremod 机械核心「范围挖掘」在 BreakEvent（NORMAL）里用 destroyBlock 连挖同种方块，不吃时运。
 * 猎宝者护符连锁时每一块都会再发 BreakEvent，若同时按着它的键，它会从这些方块出发抢先挖掉剩下的矿。
 * 连锁期间把玩家放进它自己的防重入集合 AreaMiningBoostHandler.currentlyMining，让它跳过；只在服务端主线程调用。
 */
public final class MoreModVeinCompat {
    private static final Logger LOG = LogManager.getLogger("eaddons");
    private static final String HANDLER = "com.moremod.module.handler.AreaMiningBoostHandler";

    private static boolean resolved;
    private static Set<UUID> guard;

    private MoreModVeinCompat() {
    }

    /** 返回是否由本次加入，交给 {@link #release} 配对移除。 */
    public static boolean suppress(UUID player) {
        Set<UUID> set = guard();
        return set != null && set.add(player);
    }

    public static void release(UUID player, boolean added) {
        if (added && guard != null) {
            guard.remove(player);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> guard() {
        if (!resolved) {
            resolved = true;
            if (Loader.isModLoaded("moremod")) {
                try {
                    Field field = Class.forName(HANDLER).getDeclaredField("currentlyMining");
                    field.setAccessible(true);
                    guard = (Set<UUID>) field.get(null);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.warn("moremod vein mining guard not found, the two vein miners may overlap: {}", e.toString());
                }
            }
        }
        return guard;
    }
}
