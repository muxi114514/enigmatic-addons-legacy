package net.mx.eaddons.primeval.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.primeval.PrimevalRecord;

import java.util.ArrayList;
import java.util.List;

/** 服务端 → 客户端：按下原初立方的主动键，打开记录界面，顺带告诉它当前记了哪些。 */
public class PrimevalOpenRecordMessage implements IMessage {
    private static final int MAX_ENTRIES = 16;

    private final List<String> ids = new ArrayList<>();
    private final List<Integer> bases = new ArrayList<>();

    public PrimevalOpenRecordMessage() {
    }

    public PrimevalOpenRecordMessage(List<PrimevalRecord.Entry> record) {
        for (PrimevalRecord.Entry entry : record) {
            ResourceLocation id = entry.potion.getRegistryName();
            if (id != null) {
                this.ids.add(id.toString());
                this.bases.add(entry.base);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = Math.min(buf.readByte(), MAX_ENTRIES);
        for (int i = 0; i < n; i++) {
            this.ids.add(ByteBufUtils.readUTF8String(buf));
            this.bases.add((int) buf.readShort());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.ids.size());
        for (int i = 0; i < this.ids.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, this.ids.get(i));
            buf.writeShort(this.bases.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PrimevalOpenRecordMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PrimevalOpenRecordMessage msg, MessageContext ctx) {
            net.mx.eaddons.client.PrimevalRecordClient.open(msg.ids, msg.bases);
            return null;
        }
    }
}
