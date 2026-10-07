package net.mx.eaddons.attribute;

import net.minecraft.entity.EntityLivingBase;

/**
 * 以太护盾判定：生命比例不高于以太护盾属性时生效。
 * <p>EL 的 {@code EtheriumArmor.hasShield} 经 mixin 改为调用这里，
 * 因此 EL 原有的护盾结算（减伤 ×0.5、击退攻击者、免疫投射物、音效）原样复用。
 */
public final class EtheriumShield {

    private EtheriumShield() {
    }

    public static boolean isActive(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        double threshold = EnigmaticAttributes.get(entity, EnigmaticAttributes.ETHERIUM_SHIELD);
        return threshold > 0 && entity.getHealth() <= entity.getMaxHealth() * threshold;
    }
}
