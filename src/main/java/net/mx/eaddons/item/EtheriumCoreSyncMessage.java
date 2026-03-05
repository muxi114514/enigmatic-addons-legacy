package net.mx.eaddons.item;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EtheriumCoreSyncMessage implements IMessage {
    private int cooldownRemainingTicks;
    private int shieldTicksRemaining;

    public EtheriumCoreSyncMessage() {}

    public EtheriumCoreSyncMessage(int cooldownRemainingTicks, int shieldTicksRemaining) {
        this.cooldownRemainingTicks = cooldownRemainingTicks;
        this.shieldTicksRemaining = shieldTicksRemaining;
    }

    public int getCooldownRemainingTicks() { return cooldownRemainingTicks; }
    public int getShieldTicksRemaining() { return shieldTicksRemaining; }

    @Override
    public void fromBytes(ByteBuf buf) {
        cooldownRemainingTicks = buf.readInt();
        shieldTicksRemaining = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(cooldownRemainingTicks);
        buf.writeInt(shieldTicksRemaining);
    }

    public static class Handler implements IMessageHandler<EtheriumCoreSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(EtheriumCoreSyncMessage msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                EtheriumCoreClientHelper.setFromSync(msg.getCooldownRemainingTicks(), msg.getShieldTicksRemaining());
            });
            return null;
        }
    }
}
