package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.spellstone.ShieldRaiseMessage;
import net.mx.eaddons.spellstone.SkillKeyMessage;
import net.mx.eaddons.spellstone.SwordShieldUse;
import org.lwjgl.input.Keyboard;

/**
 * 「共鸣技能键」：剑盾共鸣者按住它再右键才放形态主动，不按时右键举盾。
 * 默认左 Shift（本包键位里潜行是 Ctrl、疾跑是 Shift）；潜行键是 Shift 的玩家要在按键设置里改掉。
 */
@SideOnly(Side.CLIENT)
public class SpellstoneSkillKeyHandler {

    public static KeyBinding skillKey;

    /** 上次同步给服务端的状态；没有玩家（主菜单、断线）时归零，进服后按下才会重发。 */
    private boolean lastSent;

    public static void registerKeyBindings() {
        skillKey = new KeyBinding("key.eaddons.spellstone_skill", Keyboard.KEY_LSHIFT, "key.categories.eaddons");
        ClientRegistry.registerKeyBinding(skillKey);
    }

    public static boolean isDown() {
        return skillKey != null && skillKey.isKeyDown();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.getConnection() == null) {
            this.lastSent = false;
            return;
        }
        // 赶在原版本 tick 处理右键之前发出，服务端判定右键时已是最新状态
        boolean down = isDown();
        if (down != this.lastSent) {
            this.lastSent = down;
            EAddonsMod.PACKET_HANDLER.sendToServer(new SkillKeyMessage(down));
        }
        raiseDuringCooldown(mc, player, down);
    }

    /** 技能冷却中原版直接拦掉右键，按住右键时由这里举盾并通知服务端。 */
    private static void raiseDuringCooldown(Minecraft mc, EntityPlayerSP player, boolean skillDown) {
        if (mc.currentScreen != null || player.isHandActive() || !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemSpellstoneSword) || !SwordShieldUse.hasShield(stack)
                || !player.getCooldownTracker().hasCooldown(stack.getItem())) {
            return;
        }
        if (skillDown && SwordShieldUse.hasUsableSkill(stack)) {
            return;   // 想放技能就等冷却结束
        }
        if (SwordShieldUse.raise(player, EnumHand.MAIN_HAND, stack).getType() == net.minecraft.util.EnumActionResult.SUCCESS) {
            EAddonsMod.PACKET_HANDLER.sendToServer(new ShieldRaiseMessage());
        }
    }
}
