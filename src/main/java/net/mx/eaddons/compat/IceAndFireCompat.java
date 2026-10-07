package net.mx.eaddons.compat;

import com.github.alexthe666.iceandfire.api.IEntityEffectCapability;
import com.github.alexthe666.iceandfire.api.InFCapabilities;
import net.minecraft.entity.EntityLivingBase;

/**
 * 冰与火接缝：本模组只借它的冰壳渲染与碎冰音效，冻结逻辑一律走 {@link net.mx.eaddons.item.FrostHelper}。
 * <p>调用前必须先判 {@link ModCompat#ICE_AND_FIRE}，否则缺模组时会 NoClassDefFound。
 */
public final class IceAndFireCompat {

    private IceAndFireCompat() {
    }

    /**
     * 借冰与火画冰壳：只给 5 tick 并由 FrostHelper 每 tick 续，
     * 本模组冻结一结束就不再续，5 tick 后冰与火自行 reset 并播碎冰粒子与音效，收尾自然。
     * <p>severity 固定 0：severity≥1 会让冰与火把目标彻底定身，那是它自己的行为，本模组只要视觉。
     */
    public static void renderFrost(EntityLivingBase entity) {
        IEntityEffectCapability cap = InFCapabilities.getEntityEffectCapability(entity);
        if (cap != null) {
            cap.setFrozen(5, 0);
        }
    }

    /** 该实体是否正被冰与火冻结（冰龙冰息、冰晶爆炸等）。 */
    public static boolean isFrozenByIaf(EntityLivingBase entity) {
        IEntityEffectCapability cap = InFCapabilities.getEntityEffectCapability(entity);
        return cap != null && cap.isFrozen();
    }

    /** 清掉冰与火加的冻结（忘却冰晶佩戴者免疫冰龙冻结时使用）。 */
    public static void clearFrost(EntityLivingBase entity) {
        IEntityEffectCapability cap = InFCapabilities.getEntityEffectCapability(entity);
        if (cap != null && cap.isFrozen()) {
            cap.reset();
        }
    }
}
