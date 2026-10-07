package net.mx.eaddons.spellstone;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.mx.eaddons.EAddonsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「共鸣技能键」是否按住。右键在两端各跑一遍：客户端直接读按键，服务端读客户端按键变化时发来的状态。
 * 状态包与右键包走同一条连接、按发送顺序到达，所以服务端判定右键时看到的总是最新状态。
 */
public class SkillKeyState {

    private static final Map<UUID, Boolean> DOWN = new ConcurrentHashMap<>();

    public static boolean isDown(EntityPlayer player) {
        if (player.world.isRemote) {
            return EAddonsMod.proxy.isSpellSkillKeyDown();
        }
        return DOWN.containsKey(player.getUniqueID());
    }

    static void set(EntityPlayer player, boolean down) {
        if (down) {
            DOWN.put(player.getUniqueID(), Boolean.TRUE);
        } else {
            DOWN.remove(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DOWN.remove(event.player.getUniqueID());
    }
}
