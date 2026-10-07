package net.mx.eaddons.attribute;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 吸血与暴击伤害的结算 */
public class CombatAttributeHandler {

    /** 与 EL 原吸血同一时机（LivingDamageEvent LOW，护甲后伤害），按真实来源计算，含弹射物 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDamage(LivingDamageEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityLivingBase) || source == event.getEntityLiving() || source.world.isRemote) {
            return;
        }
        if (event.getAmount() <= 0) {
            return;
        }
        EntityLivingBase attacker = (EntityLivingBase) source;
        double lifesteal = EnigmaticAttributes.get(attacker, EnigmaticAttributes.LIFESTEAL);
        if (lifesteal > 0) {
            attacker.heal((float) (event.getAmount() * lifesteal));
        }
    }

    /** 只在确实暴击时生效（Forge 仅在暴击成立时才使用该倍率） */
    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        double bonus = EnigmaticAttributes.get(event.getEntityPlayer(), EnigmaticAttributes.CRIT_DAMAGE);
        if (bonus > 0) {
            event.setDamageModifier(event.getDamageModifier() + (float) bonus);
        }
    }
}
