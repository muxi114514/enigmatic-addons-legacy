package net.mx.eaddons.client;

import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.ItemEtheriumCore;
import net.mx.eaddons.item.EtheriumCoreTriggerMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class EtheriumCoreKeyHandler {

    public static KeyBinding etheriumCoreAbilityKey;

    public static void registerKeyBindings() {
        etheriumCoreAbilityKey = new KeyBinding("key.eaddons.etherium_core_ability", Keyboard.KEY_K, "key.categories.eaddons");
        ClientRegistry.registerKeyBinding(etheriumCoreAbilityKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.isGamePaused()) return;
        if (etheriumCoreAbilityKey.isPressed() && ItemEtheriumCore.hasEtheriumCore(mc.player)) {
            EAddonsMod.PACKET_HANDLER.sendToServer(new EtheriumCoreTriggerMessage());
        }
    }
}
