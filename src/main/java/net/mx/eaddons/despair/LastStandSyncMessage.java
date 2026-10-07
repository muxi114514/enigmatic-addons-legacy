package net.mx.eaddons.despair;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 服务端 → 客户端：绝境冷却结束的世界总时间，供证章 tooltip 显示剩余冷却。 */
public class LastStandSyncMessage implements IMessage {
    private long readyAt;

    public LastStandSyncMessage() {
    }

    public LastStandSyncMessage(long readyAt) {
        this.readyAt = readyAt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        readyAt = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(readyAt);
    }

    public static class Handler implements IMessageHandler<LastStandSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(LastStandSyncMessage message, MessageContext ctx) {
            LastStandClient.setReadyAt(message.readyAt);
            return null;
        }
    }
}
