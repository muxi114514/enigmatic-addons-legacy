package net.mx.eaddons.compat;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.SDPotions;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import net.minecraft.entity.player.EntityPlayer;

/**
 * SimpleDifficulty 兼容：焦阳护符的控温实现。
 * 仅在 {@link ModCompat#SIMPLE_DIFFICULTY} 为真时由调用方触及本类，避免未装 SD 时加载其 API。
 */
public final class SimpleDifficultyCompat {
    private SimpleDifficultyCompat() {
    }

    /**
     * 把玩家温度压到不超过 maxLevel（只降不升，不干涉低温），并清除已有的过热效果，
     * 使佩戴者永远达不到过热值、也不吃过热药水伤害。
     */
    public static void controlHeat(EntityPlayer player, int maxLevel) {
        ITemperatureCapability temperature = SDCapabilities.getTemperatureData(player);
        if (temperature == null) return;

        if (temperature.getTemperatureLevel() > maxLevel) {
            temperature.setTemperatureLevel(maxLevel);
        }
        if (SDPotions.hyperthermia != null && player.isPotionActive(SDPotions.hyperthermia)) {
            player.removePotionEffect(SDPotions.hyperthermia);
        }
    }
}
