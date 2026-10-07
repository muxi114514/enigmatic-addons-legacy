package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 忘却冰晶的被动：近战施加冻结、对冻结目标增伤、佩戴者的弹射物抗性与火焰易伤，
 * 以及全实体的冻结推进。
 *
 * <p>火焰与弹射物不走 EL 的 resistanceList：那张表按 damageType 字符串精确匹配，
 * 而火系有 in_fire/on_fire/lava/hot_floor/fireball、弹射物还有各模组自造的类型，列不全。
 * 改用原版的 {@code isFireDamage} / {@code isProjectile} 一把抓，且避免与 resistanceList 叠乘。
 */
public class ForgottenIceEventHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote) {
            return;
        }
        DamageSource source = event.getSource();

        // 1) 佩戴者受伤：弹射物减伤 + 火焰易伤（火球两者都占，按顺序各算一次）
        if (victim instanceof EntityPlayer && isWorn((EntityPlayer) victim)) {
            if (source.isFireDamage()) {
                event.setAmount(event.getAmount() * (float) ForgottenIceConfig.fireVulnerability);
            }
            if (source.isProjectile()) {
                event.setAmount(event.getAmount()
                        * (1.0F - Math.min(ForgottenIceConfig.projectileResistance, 100) / 100.0F));
            }
        }

        // 2) 佩戴者输出：先按命中前的冻结状态增伤，再施加新的冻结
        //    顺序不能反，否则第一刀就会吃到自己刚加的冻结加成
        Entity trueSource = source.getTrueSource();
        if (trueSource instanceof EntityPlayer && isWorn((EntityPlayer) trueSource)) {
            if (FrostHelper.isFrozen(victim)) {
                event.setAmount(event.getAmount()
                        * (1.0F + ForgottenIceConfig.frozenDamageBonus / 100.0F));
            }
            // 只有直接近战才冻结，弓箭/法杖等远程不触发
            if (!source.isProjectile() && !source.isMagicDamage()) {
                FrostHelper.applyFrost(victim,
                        FrostHelper.frostTimeFor(victim, ForgottenIceConfig.meleeFrostTime));
            }
        }
    }

    /** 推进所有实体身上的冻结。未被冻结的实体在 FrostHelper 里第一行就返回，开销可忽略。 */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            FrostHelper.tick(entity);
        }
    }

    private static boolean isWorn(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, ItemForgottenIce.INSTANCE) != -1;
    }
}
