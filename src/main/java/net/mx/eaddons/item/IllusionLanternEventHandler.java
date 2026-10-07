package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.mx.eaddons.util.DamageEstimate;
import net.mx.eaddons.compat.ModCompat;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * 幻影灵魂灯笼的伤害/仇恨联动，移植自 1.20 IllusionLantern 的 onHurt/onDamage 与共享处理器的不死不攻击。
 */
public class IllusionLanternEventHandler {

    /** 非魔法易伤 + 穿甲减免。 */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !ItemIllusionLantern.isWorn(player)) return;

        DamageSource source = event.getSource();
        // 跳过灯笼自身的日照诅咒，避免被自己的减伤/易伤再次修饰
        if ("evil_curse".equals(source.damageType)) return;

        boolean magic = source.damageType.contains("magic");
        if (!magic) {
            event.setAmount(event.getAmount() * IllusionLanternConfig.getNonMagicMultiplier());
        }
        if (isBypass(source)) {
            event.setAmount(event.getAmount() * IllusionLanternConfig.getBypassMultiplier());
        }
    }

    /**
     * 伤害分摊：把承受的伤害分给周围 8 格内的敌人（反射为魔法）。
     * First Aid 按部位结算时每个护甲槽都会发一次本事件；反弹已在下面的 LivingHurtEvent 做过，
     * 这里对这类玩家只再减一次自身伤害（减伤叠两次，当作特性保留），不再反弹。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        boolean reflect = !ModCompat.firstAidTakesOver(event.getEntityLiving());
        event.setAmount(share((EntityPlayer) event.getEntityLiving(), event.getSource(), event.getAmount(), false, reflect));
    }

    /**
     * 装了 First Aid 时分摊与反弹在护甲结算前做（每次受伤一次）。
     * LOW：排在上面的易伤/穿甲减免之后，又在 First Aid 的 LOWEST 之前。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurtShare(LivingHurtEvent event) {
        if (!ModCompat.firstAidTakesOver(event.getEntityLiving())) return;
        event.setAmount(share((EntityPlayer) event.getEntityLiving(), event.getSource(), event.getAmount(), true, true));
    }

    /**
     * @param preArmor amount 是护甲结算前的数值时，反弹给敌人的部分按估算的护甲后伤害算，与原设计一致
     * @param reflect  false 时只按分摊比例减自身伤害，不对敌人造成伤害
     * @return 佩戴者自己承受的伤害
     */
    private static float share(EntityPlayer player, DamageSource source, float amount, boolean preArmor,
                               boolean reflect) {
        if (isBypass(source)) return amount; // 穿甲伤害不参与分摊
        if (player.world.isRemote || !ItemIllusionLantern.isWorn(player)) return amount;

        Entity attacker = source.getTrueSource();
        List<EntityLivingBase> candidates = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, player.getEntityBoundingBox().grow(8.0));
        List<EntityLivingBase> targets = new java.util.ArrayList<>();
        for (EntityLivingBase living : candidates) {
            if (!living.isEntityAlive()) continue;
            if (living == player) continue;
            if (living == attacker) continue;
            if (living instanceof IEntityOwnable && ((IEntityOwnable) living).getOwner() == player) continue;
            if (player.isOnSameTeam(living)) continue;
            if (living instanceof EntityPlayer && ItemIllusionLantern.isWorn((EntityPlayer) living)) continue;
            targets.add(living);
        }
        if (targets.isEmpty()) return amount;

        int size = targets.size();
        float ratio = 1.0F / ((size + 1) * (size + 1));
        if (reflect) {
            float taken = preArmor ? DamageEstimate.taken(player, source, amount) : amount;
            DamageSource reflected = new EntityDamageSource("magic", player).setMagicDamage();
            for (EntityLivingBase target : targets) {
                target.attackEntityFrom(reflected, taken * ratio * size);
            }
        }
        return amount * ratio * (2 * size + 1);
    }

    /** 普通不死不主动以佩戴者为目标；亡灵中立名单里的除外。 */
    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getTarget();
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        if (mob.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD && ItemIllusionLantern.isWorn(player)
                && !IllusionLanternConfig.isTruceBlacklisted(mob)) {
            mob.setAttackTarget(null);
        }
    }

    /** 近似 1.20 的 BYPASS 标签集：无视护甲或绝对伤害。 */
    private static boolean isBypass(DamageSource source) {
        return source.isUnblockable() || source.isDamageAbsolute();
    }
}
