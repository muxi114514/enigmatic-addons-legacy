package net.mx.eaddons.spellstone;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** 原初共鸣的形态切换键：客户端按键 → 服务端换子形态。 */
public class SpellstoneFormSwitchMessage implements IMessage {

    private boolean backward;

    public SpellstoneFormSwitchMessage() {
    }

    public SpellstoneFormSwitchMessage(boolean backward) {
        this.backward = backward;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.backward = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.backward);
    }

    public static class Handler implements IMessageHandler<SpellstoneFormSwitchMessage, IMessage> {
        @Override
        public IMessage onMessage(SpellstoneFormSwitchMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            // 消息处理器跑在网络线程，实际逻辑要调度回服务端主线程
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean backward = msg.backward;
            player.getServerWorld().addScheduledTask(() -> ResonanceLink.switchForm(player, backward));
            return null;
        }
    }
}
