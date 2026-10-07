package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.spellstone.SpellstoneData;
import net.mx.eaddons.spellstone.SpellstoneFormSwitchMessage;
import org.lwjgl.input.Keyboard;

/**
 * 原初共鸣的形态切换键：默认 V 正序，潜行 + V 倒序。
 * <p>客户端只做「该不该发包」的粗判，真正的状态与合法性由服务端复核。
 */
@SideOnly(Side.CLIENT)
public class SpellstoneFormKeyHandler {

    public static KeyBinding switchFormKey;

    public static void registerKeyBindings() {
        switchFormKey = new KeyBinding("key.eaddons.spellstone_form", Keyboard.KEY_V, "key.categories.eaddons");
        ClientRegistry.registerKeyBinding(switchFormKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || switchFormKey == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.isGamePaused() || !switchFormKey.isPressed()) {
            return;
        }
        ItemStack held = mc.player.getHeldItemMainhand();
        if (held.getItem() instanceof ItemSpellstoneSword && SpellstoneData.isPrimevalActive(held)) {
            EAddonsMod.PACKET_HANDLER.sendToServer(new SpellstoneFormSwitchMessage(mc.player.isSneaking()));
        }
    }
}
