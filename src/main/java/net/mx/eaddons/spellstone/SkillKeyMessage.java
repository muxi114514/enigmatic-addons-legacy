package net.mx.eaddons.spellstone;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** 客户端「共鸣技能键」按下 / 松开时同步给服务端，见 {@link SkillKeyState}。 */
public class SkillKeyMessage implements IMessage {

    private boolean down;

    public SkillKeyMessage() {
    }

    public SkillKeyMessage(boolean down) {
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

    public static class Handler implements IMessageHandler<SkillKeyMessage, IMessage> {
        @Override
        public IMessage onMessage(SkillKeyMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean down = msg.down;
            player.getServerWorld().addScheduledTask(() -> SkillKeyState.set(player, down));
            return null;
        }
    }
}
