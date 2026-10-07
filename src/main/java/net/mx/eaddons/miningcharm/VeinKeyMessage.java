package net.mx.eaddons.miningcharm;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** 客户端「连锁挖掘键」按下 / 松开时同步给服务端，见 {@link VeinKeyState}。 */
public class VeinKeyMessage implements IMessage {

    private boolean down;

    public VeinKeyMessage() {
    }

    public VeinKeyMessage(boolean down) {
        this.down = down;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.down = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.down);
    }

    public static class Handler implements IMessageHandler<VeinKeyMessage, IMessage> {
        @Override
        public IMessage onMessage(VeinKeyMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean down = msg.down;
            player.getServerWorld().addScheduledTask(() -> VeinKeyState.set(player, down));
            return null;
        }
    }
}
