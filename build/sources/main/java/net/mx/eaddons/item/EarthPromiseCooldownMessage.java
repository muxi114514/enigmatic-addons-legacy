package net.mx.eaddons.item;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EarthPromiseCooldownMessage implements IMessage {
    private int remainingTicks;

    public EarthPromiseCooldownMessage() {}

    public EarthPromiseCooldownMessage(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        remainingTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(remainingTicks);
    }

    public static class Handler implements IMessageHandler<EarthPromiseCooldownMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(EarthPromiseCooldownMessage msg, MessageContext ctx) {
            EarthPromiseClientHelper.setCooldownFromRemainingTicks(msg.remainingTicks);
            return null;
        }
    }
}
