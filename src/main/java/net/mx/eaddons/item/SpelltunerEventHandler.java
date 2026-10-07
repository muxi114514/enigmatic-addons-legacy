package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.spellstone.KineticDamage;
import net.mx.eaddons.spellstone.SpellstoneForm;

/**
 * 术质调谐器里走事件的被动。属性类与 tick 类的在 {@link ItemSpelltuner#onWornTick}，
 * 虚空的免死在 {@link SpelltunerDeathWard}，冥灯的亡灵中立在 {@link SpelltunerUndeadTruce}。
 *
 * <p>玩家受伤的处理一律放在 {@link LivingHurtEvent} 及更早：First Aid 在 LivingHurtEvent 的 LOWEST
 * 接管玩家伤害后会取消该事件，之后的 LivingDamageEvent 对玩家根本不触发。
 */
public class SpelltunerEventHandler {

    private static final ResourceLocation PARASITES_ID = new ResourceLocation("simpledifficulty", "parasites");
    private static volatile Potion parasites;
    private static volatile boolean parasitesResolved;

    /** SimpleDifficulty 的寄生虫效果；模组不在时为 null。 */
    static Potion parasites() {
        if (!parasitesResolved) {
            parasites = ForgeRegistries.POTIONS.getValue(PARASITES_ID);
            parasitesResolved = true;
        }
        return parasites;
    }

    // ============================ 受击前：无敌窗口 / 动能 / 挑衅 ============================

    /** 虚空免死后的无敌窗口，与天使的摔落、撞墙动能免疫。在攻击事件就拦下，受击硬直与护甲损耗都不会发生。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (!(victim instanceof EntityPlayer) || victim.world.isRemote) {
            return;
        }
        DamageSource source = event.getSource();
        if (SpelltunerDeathWard.isWarded(victim, source)
                || KineticDamage.isKinetic(source) && ItemSpelltuner.hasTune(victim, SpellstoneForm.ANGEL_BLESSING)) {
            event.setCanceled(true);
        }
    }

    /** 天使：落地时直接取消摔落，伤害根本不产生。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingFall(LivingFallEvent event) {
        if (ItemSpelltuner.hasTune(event.getEntityLiving(), SpellstoneForm.ANGEL_BLESSING)) {
            event.setCanceled(true);
        }
    }

    /**
     * 冥灯：佩戴者攻击了亡灵，这一只获得反击资格。取 LOWEST 且不接收已取消的事件，被别处拦下的攻击不算；
     * 此时反击目标还没写入（原版在伤害结算之后才 setRevengeTarget）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onProvoke(LivingAttackEvent event) {
        Entity attacker = event.getSource().getTrueSource();
        if (attacker instanceof EntityPlayer && !attacker.world.isRemote) {
            SpelltunerUndeadTruce.markProvoked(event.getEntityLiving(), (EntityPlayer) attacker);
        }
    }

    // ============================ 受伤：炽焰 / 星云 / 魔像 / 海洋 / 冥灯 / 忘却 ============================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        onWearerAttack(event);
        EntityLivingBase victim = event.getEntityLiving();
        if (!ItemSpelltuner.isWearing(victim)) {
            return;
        }
        DamageSource source = event.getSource();
        // 炽焰：免疫火焰伤害，但岩浆照打（弱化版，本体术石连岩浆也免）
        if (source.isFireDamage() && !DamageSource.LAVA.damageType.equals(source.damageType)
                && ItemSpelltuner.hasTune(victim, SpellstoneForm.BLAZING_CORE)) {
            event.setCanceled(true);
            return;
        }
        Entity attacker = source.getTrueSource();
        boolean melee = attacker instanceof EntityLivingBase && attacker != victim
                && !source.isProjectile() && !source.isMagicDamage();
        float amount = event.getAmount();
        // 星云：魔法伤害减免。魔法伤害大多无视护甲，放在护甲结算前后差别很小
        if (source.isMagicDamage() && ItemSpelltuner.hasTune(victim, SpellstoneForm.EYE_OF_NEBULA)) {
            amount *= percentLeft(SpelltunerConfig.nebulaMagicReduction);
        }
        if (melee && ItemSpelltuner.hasTune(victim, SpellstoneForm.GOLEM_HEART)) {
            amount *= percentLeft(SpelltunerConfig.golemMeleeReduction);
        }
        if (victim.isWet() && ItemSpelltuner.hasTune(victim, SpellstoneForm.OCEAN_STONE)) {
            amount *= percentLeft(SpelltunerConfig.oceanWetReduction);
        }
        if (attacker instanceof EntityLivingBase
                && ((EntityLivingBase) attacker).getCreatureAttribute() == EnumCreatureAttribute.UNDEAD
                && ItemSpelltuner.hasTune(victim, SpellstoneForm.ILLUSION_LANTERN)) {
            amount *= percentLeft(SpelltunerConfig.lanternUndeadReduction);
        }
        event.setAmount(amount);
        // 忘却：受近战攻击时冻结攻击者
        if (melee && ItemSpelltuner.hasTune(victim, SpellstoneForm.FORGOTTEN_ICE)) {
            EntityLivingBase living = (EntityLivingBase) attacker;
            FrostHelper.applyFrost(living, FrostHelper.frostTimeFor(living, SpelltunerConfig.frostFreezeTicks));
        }
    }

    /** 佩戴者近战出手：天使在空中增伤、烈焰点燃目标。 */
    private static void onWearerAttack(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityPlayer) || attacker == event.getEntityLiving()
                || source.getImmediateSource() != attacker || source.isProjectile() || source.isMagicDamage()
                || !ItemSpelltuner.isWearing((EntityPlayer) attacker)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) attacker;
        if (!player.onGround && player.world.isAirBlock(player.getPosition())
                && ItemSpelltuner.hasTune(player, SpellstoneForm.ANGEL_BLESSING)) {
            event.setAmount(event.getAmount() * (1.0F + SpelltunerConfig.angelAirBonus / 100.0F));
        }
        if (SpelltunerConfig.blazeIgniteSeconds > 0 && ItemSpelltuner.hasTune(player, SpellstoneForm.BLAZING_CORE)) {
            event.getEntityLiving().setFire(SpelltunerConfig.blazeIgniteSeconds);
        }
    }

    private static float percentLeft(int reduction) {
        return Math.max(0.0F, 1.0F - reduction / 100.0F);
    }

    // ============================ 魔像：完全免疫击退 ============================

    /** 抗击退属性已拉到 1.0；再取消击退事件，免得别处的负抗击退修饰符把总值拉回 1 以下。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKnockBack(LivingKnockBackEvent event) {
        if (ItemSpelltuner.hasTune(event.getEntityLiving(), SpellstoneForm.GOLEM_HEART)) {
            event.setCanceled(true);
        }
    }

    // ============================ 海洋：免疫寄生虫 ============================

    /** 阻断层：寄生虫根本加不上去。清掉戴上前已有寄生虫的清理层在 onWornTick。 */
    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        Potion p = parasites();
        if (p != null && event.getPotionEffect().getPotion() == p
                && ItemSpelltuner.hasTune(event.getEntityLiving(), SpellstoneForm.OCEAN_STONE)) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ============================ 引擎：物品使用提速 ============================

    /**
     * 按住右键使用物品时，按进度隔几格多推进 1 格。
     * <p>用时减少 r 等于速度 ×1/(1-r)，一个「多推 1 格」的循环在进度上跨 1/r 格，所以按
     * 「已用进度 % round(1/r) == 0」判定：r=20% 时每 4 tick 推进 5 格，正好 ×1.25。
     * 若按剩余时间取模，跳格会改变之后的落点，实际变成 ×1.33。判定只看进度，两端算得一致，不必存状态。
     * <p>剩余 ≤2 时不跳：原版要把计数从 1 递减到恰好 0 才触发 onItemUseFinish，跳过 0 就永远用不完。
     */
    @SubscribeEvent
    public void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        int reduction = SpelltunerConfig.engineUseTimeReduction;
        int left = event.getDuration();
        if (reduction <= 0 || left <= 2
                || !ItemSpelltuner.hasTune(event.getEntityLiving(), SpellstoneForm.LOST_ENGINE)) {
            return;
        }
        int period = Math.max(1, Math.round(100.0F / reduction));
        int progress = event.getItem().getMaxItemUseDuration() - left;
        if (progress % period == 0) {
            event.setDuration(left - 1);
        }
    }

    // ============================ 冥灯：普通亡灵中立 ============================

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        SpelltunerUndeadTruce.onSetTarget(event.getEntityLiving(), event.getTarget());
    }
}
