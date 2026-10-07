package net.mx.eaddons.miningcharm;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端记录谁按着「连锁挖掘键」。客户端只在按下 / 松开时发包，
 * 状态包与挖掘包走同一条连接、按发送顺序到达，判定破坏方块时看到的总是最新状态。
 */
public class VeinKeyState {

    private static final Set<UUID> DOWN = ConcurrentHashMap.newKeySet();

    public static boolean isDown(EntityPlayer player) {
        return DOWN.contains(player.getUniqueID());
    }

    static void set(EntityPlayer player, boolean down) {
        if (down) {
            DOWN.add(player.getUniqueID());
        } else {
            DOWN.remove(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DOWN.remove(event.player.getUniqueID());
    }
}
