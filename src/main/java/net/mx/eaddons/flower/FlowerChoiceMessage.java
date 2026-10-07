package net.mx.eaddons.flower;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 客户端 → 服务端：石英花界面里用术质核心选中的属性或效果。只传选择，校验与扣核心都在服务端。 */
public class FlowerChoiceMessage implements IMessage {
    private static final int MAX_ID_LENGTH = 256;

    private int windowId;
    private boolean effect;
    private int index;
    private String id = "";

    public FlowerChoiceMessage() {
    }

    public FlowerChoiceMessage(int windowId, boolean effect, int index, String id) {
        this.windowId = windowId;
        this.effect = effect;
        this.index = index;
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.effect = buf.readBoolean();
        this.index = buf.readByte();
        this.id = ByteBufUtils.readUTF8String(buf);
        if (this.id.length() > MAX_ID_LENGTH) {
            this.id = "";
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.effect);
        buf.writeByte(this.index);
        ByteBufUtils.writeUTF8String(buf, this.id);
    }

    public static class Handler implements IMessageHandler<FlowerChoiceMessage, IMessage> {
        @Override
        public IMessage onMessage(FlowerChoiceMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            // 网络线程收到，切回服务端主线程再改物品
            player.getServerWorld().addScheduledTask(
                    () -> FlowerCoreChoice.apply(player, msg.windowId, msg.effect, msg.index, msg.id));
            return null;
        }
    }
}
