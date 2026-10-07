package net.mx.eaddons.spellstone;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.mx.eaddons.item.ItemSpellstoneSword;

/**
 * 技能冷却期间举盾。原版在冷却中直接拦掉右键（连 onItemRightClick 都不调用），
 * 客户端检测到按住右键时自己先举盾，再用这个包让服务端同步举盾；松手仍走原版的停止使用流程。
 */
public class ShieldRaiseMessage implements IMessage {

    public ShieldRaiseMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<ShieldRaiseMessage, IMessage> {
        @Override
        public IMessage onMessage(ShieldRaiseMessage msg, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof ItemSpellstoneSword && SwordShieldUse.hasShield(stack)
                        && !player.isHandActive()) {
                    SwordShieldUse.raise(player, EnumHand.MAIN_HAND, stack);
                }
            });
            return null;
        }
    }
}
