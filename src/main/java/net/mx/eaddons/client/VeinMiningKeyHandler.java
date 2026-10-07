package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.miningcharm.VeinKeyMessage;
import org.lwjgl.input.Keyboard;

/**
 * 猎宝者护符的「连锁挖掘键」：按住时挖掉的方块会连带相连的同种方块。
 * 默认 Caps Lock，与整合包里 moremod 范围挖掘键同一个键，按一个键两边都生效。
 */
@SideOnly(Side.CLIENT)
public class VeinMiningKeyHandler {

    public static KeyBinding veinKey;

    /** 上次同步给服务端的状态；没有玩家（主菜单、断线）时归零，进服后按下才会重发。 */
    private boolean lastSent;

    public static void registerKeyBindings() {
        veinKey = new KeyBinding("key.eaddons.vein_mining", KeyConflictContext.IN_GAME, Keyboard.KEY_CAPITAL,
                "key.categories.eaddons");
        ClientRegistry.registerKeyBinding(veinKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.getConnection() == null) {
            this.lastSent = false;
            return;
        }
        boolean down = veinKey.isKeyDown();
        if (down != this.lastSent) {
            this.lastSent = down;
            EAddonsMod.PACKET_HANDLER.sendToServer(new VeinKeyMessage(down));
        }
    }
}
