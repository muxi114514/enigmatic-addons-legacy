package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 混沌鞘翅配置。移植自 1.20 神遗拓展 ChaosElytra 的 onConfig。
 */
public class ChaosElytraConfig {
    private static final String CATEGORY = "ChaosElytra";

    /** 加速冲刺时的飞行速度倍率。 */
    public static double flyingSpeedModifier = 1.6;
    /** 混沌俯冲砸地的伤害倍率（按下落动量取幂）。 */
    public static double descendingPowerModifier = 1.6;
    /** 混沌俯冲冷却（tick）。 */
    public static int descendingCooldown = 500;
    /** 定向减伤百分比。 */
    public static int damageResistancePercent = 80;

    public static void init(Configuration config) {
        flyingSpeedModifier = config.getFloat("FlyingSpeedModifier", CATEGORY, 1.6F, 1F, 10F,
                "Flying speed multiplier when boosting.");

        descendingPowerModifier = config.getFloat("DescendingPowerModifier", CATEGORY, 1.6F, 1F, 10F,
                "Damage multiplier when slamming the ground with a boosted descent.");

        descendingCooldown = config.getInt("DescendingCooldown", CATEGORY, 500, 200, 2400,
                "Cooldown (ticks) of the Chaos Descending slam.");

        damageResistancePercent = config.getInt("DamageResistance", CATEGORY, 80, 0, 100,
                "Directional damage resistance (percent) while worn.");
    }

    /** 减伤倍率（0..1，即减免比例）。 */
    public static float getDamageResistance() {
        return damageResistancePercent / 100.0F;
    }
}
