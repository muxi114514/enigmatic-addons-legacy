package net.mx.eaddons.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * 专用服务端 {@code allow-flight=false} 时，玩家悬空（非下落）累计超过 80 tick 会被踢出
 * 「本服务器未启用飞行」。计数在 {@code NetHandlerPlayServer.floatingTickCount}，
 * 合法的悬空状态（如挂在抓钩上）每 tick 清零即可；整合服默认允许飞行，不受影响。
 */
public final class FloatingKickGuard {

    private static final Logger LOG = LogManager.getLogger("eaddons");

    private static Field floatingTickCount;
    private static boolean unavailable;

    private FloatingKickGuard() {
    }

    /** 只能在服务端主线程调用。 */
    public static void reset(EntityPlayerMP player) {
        if (unavailable || player.connection == null) {
            return;
        }
        try {
            if (floatingTickCount == null) {
                floatingTickCount = ObfuscationReflectionHelper.findField(NetHandlerPlayServer.class, "field_147365_f");
            }
            floatingTickCount.setInt(player.connection, 0);
        } catch (Exception e) {
            unavailable = true;   // 反射失败只记一次，之后不再尝试
            LOG.warn("Cannot reset floatingTickCount, hanging players may be kicked for flying", e);
        }
    }
}
