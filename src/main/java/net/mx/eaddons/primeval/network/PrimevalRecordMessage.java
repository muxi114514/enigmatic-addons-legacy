package net.mx.eaddons.primeval.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.mx.eaddons.primeval.PrimevalRecord;

import java.util.ArrayList;
import java.util.List;

/** 客户端 → 服务端：关闭记录界面时提交选中的效果，或清空记录。只传效果 id，等级由服务端按实际效果算。 */
public class PrimevalRecordMessage implements IMessage {
    private static final int MAX_IDS = 16;

    private boolean clear;
    private final List<String> ids = new ArrayList<>();

    public PrimevalRecordMessage() {
    }

    public PrimevalRecordMessage(List<String> ids, boolean clear) {
        this.ids.addAll(ids);
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.clear = buf.readBoolean();
        int n = Math.min(buf.readByte(), MAX_IDS);
        for (int i = 0; i < n; i++) {
            this.ids.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.clear);
        buf.writeByte(this.ids.size());
        for (String id : this.ids) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    public static class Handler implements IMessageHandler<PrimevalRecordMessage, IMessage> {
        @Override
        public IMessage onMessage(PrimevalRecordMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            // 网络线程收到，切回服务端主线程再动物品与药水
            player.getServerWorld().addScheduledTask(() -> PrimevalRecord.applySelection(player, msg.ids, msg.clear));
            return null;
        }
    }
}
