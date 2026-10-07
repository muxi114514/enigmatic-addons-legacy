package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端按实际位移逐 tick 测玩家速度（格/tick）。
 *
 * <p>玩家移动由客户端决定，服务端 {@code EntityPlayerMP} 的 motion 不随之更新（水平方向基本为 0，
 * 只有服务端自己击退或设速度时才有值），直接读 motion 会让「按移动速度增伤」几乎恒为 0。
 * 这里在玩家刻末尾比对上一 tick 的坐标；非玩家生物的 motion 由服务端模拟，照旧读 motion。
 */
public final class PlayerSpeedTracker {

    /** 单 tick 位移平方超过它（10 格）视为传送，不计速度。 */
    private static final double TELEPORT_SQ = 100.0D;

    /** 每个玩家：{上一 tick 的 x, y, z, 水平速度, 合速度}。只在服务端主线程读写。 */
    private static final Map<UUID, double[]> TRACKS = new ConcurrentHashMap<>();

    public static double horizontalSpeed(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) {
            return Math.sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
        }
        double[] track = TRACKS.get(entity.getUniqueID());
        return track == null ? 0.0D : track[3];
    }

    public static double speed(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) {
            return Math.sqrt(entity.motionX * entity.motionX + entity.motionY * entity.motionY
                    + entity.motionZ * entity.motionZ);
        }
        double[] track = TRACKS.get(entity.getUniqueID());
        return track == null ? 0.0D : track[4];
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER) {
            return;
        }
        EntityPlayer player = event.player;
        double[] track = TRACKS.get(player.getUniqueID());
        if (track == null) {
            TRACKS.put(player.getUniqueID(), new double[]{player.posX, player.posY, player.posZ, 0.0D, 0.0D});
            return;
        }
        double dx = player.posX - track[0];
        double dy = player.posY - track[1];
        double dz = player.posZ - track[2];
        double horizontalSq = dx * dx + dz * dz;
        double totalSq = horizontalSq + dy * dy;
        boolean teleported = totalSq > TELEPORT_SQ;
        track[0] = player.posX;
        track[1] = player.posY;
        track[2] = player.posZ;
        track[3] = teleported ? 0.0D : Math.sqrt(horizontalSq);
        track[4] = teleported ? 0.0D : Math.sqrt(totalSq);
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.player.getUniqueID());
    }

    /** 换维度 / 重生后坐标会跳变，丢掉旧记录从头测。 */
    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        TRACKS.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        TRACKS.remove(event.player.getUniqueID());
    }
}
