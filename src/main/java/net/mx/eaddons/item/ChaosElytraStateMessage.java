package net.mx.eaddons.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 客户端→服务端：同步混沌鞘翅状态。
 * action：0=停止加速，1=开始加速，2=请求开启鞘翅飞行。
 */
public class ChaosElytraStateMessage implements IMessage {
    public static final int STOP_BOOST = 0;
    public static final int START_BOOST = 1;
    public static final int START_FLY = 2;

    private int action;

    public ChaosElytraStateMessage() {
    }

    public ChaosElytraStateMessage(int action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.action);
    }

    public static class Handler implements IMessageHandler<ChaosElytraStateMessage, IMessage> {
        @Override
        public IMessage onMessage(ChaosElytraStateMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) return null;
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final int action = msg.action;
            player.getServerWorld().addScheduledTask(() -> {
                if (ChaosElytraHelper.getChaosElytra(player).isEmpty()) return;
                switch (action) {
                    case START_FLY:
                        // 记录"飞行意图"：由服务端 tick 每帧维持 flag7，避免元数据同步把状态刷掉（一顿一顿）。
                        // 立即置位一次求响应，落地/入水/脱下时在 tick 里清除意图并退出。
                        player.getEntityData().setBoolean("ChaosElytraWantsFly", true);
                        if (!player.onGround && !player.isElytraFlying() && !player.isInWater()) {
                            ChaosElytraHelper.setElytraFlying(player, true);
                        }
                        break;
                    case START_BOOST:
                        player.getEntityData().setBoolean("ChaosElytraBoosting", true);
                        break;
                    case STOP_BOOST:
                        player.getEntityData().setBoolean("ChaosElytraBoosting", false);
                        break;
                    default:
                        break;
                }
            });
            return null;
        }
    }
}
