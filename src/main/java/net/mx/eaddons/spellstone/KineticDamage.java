package net.mx.eaddons.spellstone;

import net.minecraft.util.DamageSource;

/**
 * 「动能」伤害：摔落与撞墙（鞘翅或被甩飞时高速撞墙）。落石砸中不算。
 * 失落引擎共鸣者与天使调谐共用。
 */
public final class KineticDamage {

    private KineticDamage() {
    }

    /** 按伤害类型名比对而不是比对实例，别的模组自己 new 出来的同名伤害也能认出。 */
    public static boolean isKinetic(DamageSource source) {
        String type = source.getDamageType();
        return DamageSource.FALL.getDamageType().equals(type)
                || DamageSource.FLY_INTO_WALL.getDamageType().equals(type);
    }
}
