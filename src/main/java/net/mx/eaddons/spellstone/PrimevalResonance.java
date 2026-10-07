package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.mx.eaddons.item.ItemAntiqueBag;
import net.mx.eaddons.item.ItemTheBless;
import net.mx.eaddons.item.SpellstoneSwordConfig;
import net.mx.eaddons.potion.PotionIchorCorrosion;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 原初共鸣的效果。
 *
 * <p>命中效果只属于纯原初形态：施加灵液腐蚀，书袋里有恩惠之典时提一级。
 * 其余几条是原初共鸣的通用被动，切到任何子形态都生效：
 * 削目标无敌帧（本类）、全形态技能冷却 -25%（{@link SwordCooldown}）、
 * 抵消禁忌之果与地狱刃片护符的负面（{@link ResonanceEvents} 与饰品本体）。
 */
public final class PrimevalResonance {

    /**
     * 「书袋里有没有恩惠之典」的缓存。{@code ItemAntiqueBag.hasItemInBag} 要遍历背包与末影箱
     * 并反序列化袋内物品，不能每次命中都查。弱引用键，玩家实体被丢弃后条目自然消失。
     */
    private static final Map<EntityPlayer, long[]> BLESS_CACHE =
            Collections.synchronizedMap(new WeakHashMap<EntityPlayer, long[]>());

    private static final int CACHE_TICKS = 20;

    private PrimevalResonance() {
    }

    /** 命中时的原初效果。由 {@link SpellstoneAbilities#onHitEntity} 统一调用。 */
    public static void onHit(ItemStack sword, EntityLivingBase target, EntityLivingBase attacker) {
        if (!SpellstoneData.isPrimevalActive(sword)) {
            return;
        }
        // 削无敌帧：原版受击后 20 tick 内不吃满伤害（前 10 tick 只结算差额），
        // 压低上限等于缩短这个窗口，高攻速形态才不会有半数攻击被吞掉。
        int frames = SpellstoneSwordConfig.primevalHurtResistant;
        if (target.hurtResistantTime > frames) {
            target.hurtResistantTime = frames;
        }
        if (SpellstoneData.getSubForm(sword) != SpellstoneForm.PRIMEVAL_CUBE) {
            return;
        }
        int level = SpellstoneSwordConfig.primevalCorrosionLevel;
        if (attacker instanceof EntityPlayer && hasBlessInBag((EntityPlayer) attacker)) {
            level = SpellstoneSwordConfig.primevalCorrosionBlessLevel;
        }
        // 等级高的会顶掉书袋自带的 I 级；受伤加成那部分由恩惠之典的事件按等级统一结算
        target.addPotionEffect(new PotionEffect(PotionIchorCorrosion.INSTANCE,
                SpellstoneSwordConfig.primevalCorrosionTime, Math.max(0, level - 1), false, true));
    }

    private static boolean hasBlessInBag(EntityPlayer player) {
        long now = player.world.getTotalWorldTime();
        long[] cached = BLESS_CACHE.get(player);
        if (cached != null && cached[0] > now) {
            return cached[1] != 0;
        }
        boolean has = ItemAntiqueBag.hasItemInBag(player, ItemTheBless.INSTANCE);
        BLESS_CACHE.put(player, new long[]{now + CACHE_TICKS, has ? 1L : 0L});
        return has;
    }
}
