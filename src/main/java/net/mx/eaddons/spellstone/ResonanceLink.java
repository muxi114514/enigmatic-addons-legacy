package net.mx.eaddons.spellstone;

import baubles.api.BaublesApi;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.entity.EntityEngineHook;
import net.mx.eaddons.item.ItemSpellstoneSword;

import java.util.UUID;

/**
 * 两颗立方的「链接式共鸣」：共鸣时不吃入术石，只在剑上记下形态，术石留给玩家继续佩戴，
 * 于是术石本身的饰品效果和剑的形态效果可以同时生效。
 *
 * <p>代价是形态的生效与否取决于「有没有戴着那颗术石」，而 1.12.2 的
 * {@code Item#getAttributeModifiers} 拿不到持有者，所以佩戴状态要落到剑的 NBT 上
 * （{@link SpellstoneData#setLinkActive}），戴上/摘下的那一刻 ItemStack 变化，
 * 属性修饰符才会跟着重算。
 */
public final class ResonanceLink {

    private ResonanceLink() {
    }

    // ============================ 佩戴判定 ============================

    /** 玩家的饰品栏里是否戴着该形态对应的术石。 */
    public static boolean isWearing(EntityPlayer player, SpellstoneForm form) {
        if (form == null || form.itemId() == null) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(form.itemId());
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    /**
     * 刷新佩戴状态。只对拿在手上的剑调用：没拿在手上时属性与技能都用不着，
     * 每把背包里的剑都去翻饰品栏没有意义。
     */
    public static void refresh(EntityPlayer player, ItemStack sword) {
        SpellstoneForm link = SpellstoneData.getLinkForm(sword);
        if (link == SpellstoneForm.NONE) {
            return;
        }
        boolean wearing = isWearing(player, link);
        if (wearing != SpellstoneData.isLinkActive(sword)) {
            SpellstoneData.setLinkActive(sword, wearing);
            if (!wearing) {
                retractHook(player);
            }
        }
    }

    /**
     * 主手是否握着一把立方共鸣生效中的共鸣者。
     * <p>两个立方共有的两条豁免（禁忌之果、地狱刃片护符）都以此为判定。
     */
    public static boolean holdsActiveLink(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemSpellstoneSword)) {
            return false;
        }
        return SpellstoneData.isPrimevalActive(held) || SpellstoneData.isCubeActive(held);
    }

    // ============================ 建立 / 解除 ============================

    /** 建立链接：术石留在副手不动，剑直接记为满级。 */
    public static void link(EntityPlayer player, ItemStack sword, SpellstoneForm form) {
        SpellstoneData.setLink(sword, form);
        SpellstoneData.setLevel(sword, SpellstoneData.MAX_LEVEL);
        SpellstoneData.resetEnergy(sword);
        SpellstoneData.setLinkActive(sword, isWearing(player, form));
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
        if (!SpellstoneData.isLinkActive(sword)) {
            player.sendStatusMessage(new TextComponentTranslation("message.eaddons.resonance.dormant"), true);
        }
    }

    /** 解除链接：不吐出任何东西，等级保留。 */
    public static void unlink(EntityPlayer player, ItemStack sword) {
        retractHook(player);
        SpellstoneData.clearLink(sword);
        SpellstoneData.resetEnergy(sword);
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.8F);
    }

    // ============================ 子形态切换 ============================

    /** 按键切换原初共鸣的子形态。返回是否真的切了。 */
    public static boolean switchForm(EntityPlayer player, boolean backward) {
        ItemStack sword = player.getHeldItemMainhand();
        if (!(sword.getItem() instanceof ItemSpellstoneSword) || !SpellstoneData.isPrimevalActive(sword)) {
            return false;
        }
        SpellstoneForm next = SpellstoneForm.cycle(SpellstoneData.getSubForm(sword), backward);
        retractHook(player);
        player.resetActiveHand();
        SpellstoneData.setSubForm(sword, next);
        SpellstoneData.resetEnergy(sword);
        player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 0.7F, backward ? 0.8F : 1.4F);
        // 翻译交给客户端做：服务端这边不碰 I18n
        player.sendStatusMessage(new TextComponentTranslation("message.eaddons.resonance.switch",
                new TextComponentTranslation("item.spellstone_sword." + formNameKey(next) + ".name")), true);
        return true;
    }

    /** 子形态在语言文件里的键：纯原初用立方自己的名字，其余复用十形态原有的名字。 */
    public static String formNameKey(SpellstoneForm form) {
        return form == SpellstoneForm.PRIMEVAL_CUBE ? "primeval_spellstone" : form.path();
    }

    /** 换形态、摘下术石、解除链接时都要把放出去的抓钩收回，免得留下无主的钩子。 */
    private static void retractHook(EntityLivingBase owner) {
        UUID uuid = SpellstoneData.getPlayerEngineHook(owner);
        if (uuid == null || !(owner.world instanceof WorldServer)) {
            return;
        }
        net.minecraft.entity.Entity hook = ((WorldServer) owner.world).getEntityFromUuid(uuid);
        if (hook instanceof EntityEngineHook) {
            hook.setDead();
        }
        SpellstoneData.setPlayerEngineHook(owner, null);
    }
}
