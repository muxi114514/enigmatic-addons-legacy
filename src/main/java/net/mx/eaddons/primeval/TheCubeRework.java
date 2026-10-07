package net.mx.eaddons.primeval;

import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.mx.eaddons.util.PotionRefresh;

import java.util.ArrayList;
import java.util.List;

/**
 * 非欧立方的新主动（替换原来的「随机传送到另一个维度」，见 MixinItemTheCube）：
 * 身上所有正面效果延长到 60 分钟（只延长不缩短），并各提升一级（不超过配置上限）。
 * 身上没有正面效果时不触发、不进冷却。
 * <p>「正面」取「非负面」（{@code !isBadEffect}）：不少模组药水没调 setBeneficial，只看 isBeneficial 会漏掉它们。
 */
public final class TheCubeRework {

    private TheCubeRework() {
    }

    public static void activate(EntityPlayerMP player) {
        IForbiddenConsumed cap = IForbiddenConsumed.get(player);
        if (cap.getSpellstoneCooldown() > 0) {
            return;
        }
        List<PotionEffect> buffs = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!effect.getPotion().isBadEffect()) {
                buffs.add(effect);
            }
        }
        if (buffs.isEmpty()) {
            return;
        }

        int duration = PrimevalConfig.cubeBuffDuration;
        for (PotionEffect effect : buffs) {
            int amplifier = effect.getAmplifier();
            if (amplifier < PrimevalConfig.buffLevelCap) {
                PotionRefresh.upgrade(player, effect, amplifier + 1, Math.max(duration, effect.getDuration()));
            } else {
                PotionRefresh.extend(player, effect, duration);
            }
        }

        cap.setSpellstoneCooldown(PrimevalConfig.cubeCooldown);
        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ILLAGER_CAST_SPELL,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        player.getServerWorld().spawnParticle(EnumParticleTypes.SPELL_MOB, player.posX,
                player.posY + player.height * 0.5D, player.posZ, 40, 0.4D, 0.6D, 0.4D, 1.0D);
    }
}
