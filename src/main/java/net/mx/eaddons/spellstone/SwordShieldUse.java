package net.mx.eaddons.spellstone;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.mx.eaddons.compat.ChocoTweakShield;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 共鸣者合成「剑盾」后的右键分流：普通右键举盾，按住共鸣技能键再右键才放形态主动。
 *
 * <p>chocotweak 让带盾的剑能举盾，靠的是注入原版 {@code Item} 基类的 onItemRightClick / getItemUseAction /
 * getMaxItemUseDuration；共鸣者重写了这三个方法又不调父类，注入点永远走不到，所以改由这里接管。
 *
 * <p>同一把剑的一次「使用」可能是举盾也可能是技能蓄力，靠技能起手时给物品实例打标记区分：
 * 使用期间实例不变（换了实例由 {@code canContinueUsing} 转移标记），而改 NBT 会让客户端换实例并打断使用，不能用。
 */
public final class SwordShieldUse {

    /** 正在蓄力技能的物品实例。ItemStack 没有重写 equals，按实例比较、弱引用；客户端与整合服线程都会访问，加同步。 */
    private static final Map<ItemStack, Boolean> SKILL_USES = Collections.synchronizedMap(new WeakHashMap<>());

    private SwordShieldUse() {
    }

    public static boolean hasShield(ItemStack stack) {
        return ChocoTweakShield.hasShield(stack);
    }

    /** 这次使用是盾牌格挡，而不是技能蓄力。 */
    public static boolean isShieldUse(ItemStack stack) {
        return hasShield(stack) && !SKILL_USES.containsKey(stack);
    }

    /** 技能蓄力起手：先标记再进入使用，带盾时 getItemUseAction 等才会按形态返回。 */
    public static void startSkillUse(EntityPlayer player, EnumHand hand, ItemStack stack) {
        if (hasShield(stack)) {
            SKILL_USES.put(stack, Boolean.TRUE);
        }
        player.setActiveHand(hand);
    }

    public static void endUse(ItemStack stack) {
        SKILL_USES.remove(stack);
    }

    /** 使用中换了物品实例（服务端改了 NBT 或耐久），把技能标记带到新实例上。 */
    public static void transfer(ItemStack from, ItemStack to) {
        if (from != to && SKILL_USES.remove(from) != null) {
            SKILL_USES.put(to, Boolean.TRUE);
        }
    }

    /** 这次右键该举盾：没按技能键，或按了但当前形态没有可放的主动。 */
    public static boolean wantsBlock(EntityPlayer player, ItemStack stack) {
        return hasShield(stack) && (!SkillKeyState.isDown(player) || !hasUsableSkill(stack));
    }

    /** 与 chocotweak 一致：疲倦时不能举盾；副手有东西时不举盾，除非有「提灯」觉醒。 */
    public static boolean canRaise(EntityPlayer player, ItemStack stack) {
        return !ChocoTweakShield.isExhausted(player)
                && (player.getHeldItemOffhand().isEmpty() || ChocoTweakShield.canDualWieldBlock(stack));
    }

    public static ActionResult<ItemStack> raise(EntityPlayer player, EnumHand hand, ItemStack stack) {
        if (!canRaise(player, stack)) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        endUse(stack);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** 当前形态此刻有没有右键主动可放；没有的话技能键 + 右键也照样举盾。 */
    public static boolean hasUsableSkill(ItemStack stack) {
        int energy = SpellstoneData.getEnergy(stack);
        switch (SpellstoneData.getForm(stack)) {
            case BLAZING_CORE:
                return energy > 0;
            case OCEAN_STONE: {
                int max = SpellstoneData.getMaxEnergy(stack);
                return max > 0 && energy >= max;
            }
            case VOID_PEARL:
            case THE_CUBE:
            case ANGEL_BLESSING:
            case ILLUSION_LANTERN:
            case EYE_OF_NEBULA:
            case LOST_ENGINE:
                return true;
            default:
                return false;   // 魔像、忘却、复苏、纯原初没有右键主动
        }
    }
}
