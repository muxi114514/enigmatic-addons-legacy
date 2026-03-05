package net.mx.eaddons.item;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class EtheriumCoreTriggerMessage implements IMessage {

    public EtheriumCoreTriggerMessage() {}

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<EtheriumCoreTriggerMessage, IMessage> {
        @Override
        public IMessage onMessage(EtheriumCoreTriggerMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) return null;
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EtheriumCoreEventHandler.onTrigger(ctx.getServerHandler().player);
            });
            return null;
        }
    }
}
