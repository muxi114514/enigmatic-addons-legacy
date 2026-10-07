package net.mx.eaddons.spellstone;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 挂绳时按跳跃跃出：绳索物理在持有者客户端，跃出也由客户端发起，这里通知服务端把钩收掉。
 * 只收发送者自己登记的那只钩，无需携带参数。
 */
public class EngineHookDetachMessage implements IMessage {

    public EngineHookDetachMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<EngineHookDetachMessage, IMessage> {
        @Override
        public IMessage onMessage(EngineHookDetachMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> SpellstoneActives.retractHook(player));
            return null;
        }
    }
}
