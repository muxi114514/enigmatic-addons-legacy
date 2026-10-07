package net.mx.eaddons.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * 给予 / 续期 / 升级药水效果，且不借机重套属性修饰符。
 *
 * <p>原版 {@code addPotionEffect} 碰上已有的同种效果时，不论有没有变化都会走 {@code onChangedPotionEffect}
 * 把属性修饰符先扣后加：伤害吸收因此被补满护盾，生命提升在扣修饰符那一下把当前生命截到基础上限。
 * 所以这里只在「身上没有 / 需要升级」时才走 addPotionEffect；同等级续期直接改剩余时间，再单独同步给客户端。
 */
public final class PotionRefresh {

    /** {@code PotionEffect#duration}（SRG 名）。找不到时退回原版续期，至少效果不会断。 */
    @Nullable
    private static final Field DURATION = findDurationField();

    private PotionRefresh() {
    }

    /**
     * 保证实体身上有不低于 amplifier 级的该效果。身上已有更高等级（比如喝了更强的药水）就不动。
     *
     * @param duration  新施加或续期后的时长
     * @param refreshAt 同等级时剩余不超过它才续期，避免每 tick 发包
     */
    public static void ensure(EntityLivingBase entity, Potion potion, int amplifier, int duration, int refreshAt,
                              boolean ambient, boolean particles) {
        PotionEffect existing = entity.getActivePotionEffect(potion);
        if (existing == null) {
            entity.addPotionEffect(new PotionEffect(potion, duration, amplifier, ambient, particles));
        } else if (existing.getAmplifier() < amplifier) {
            upgrade(entity, existing, amplifier, duration);
        } else if (existing.getAmplifier() == amplifier && existing.getDuration() <= refreshAt) {
            setDuration(entity, existing, duration);
        }
    }

    /** 把剩余时间延长到 duration（只延长不缩短），不重套属性。 */
    public static void extend(EntityLivingBase entity, PotionEffect effect, int duration) {
        if (effect.getDuration() < duration) {
            setDuration(entity, effect, duration);
        }
    }

    /**
     * 升到 amplifier 级。等级变化只能走 addPotionEffect（属性要按新等级重算），
     * 前后保住当前生命；伤害吸收只加上升级带来的差额，不借机回满。
     */
    public static void upgrade(EntityLivingBase entity, PotionEffect existing, int amplifier, int duration) {
        Potion potion = existing.getPotion();
        int oldAmplifier = existing.getAmplifier();
        float health = entity.getHealth();
        float absorption = entity.getAbsorptionAmount();

        entity.addPotionEffect(new PotionEffect(potion, duration, amplifier,
                existing.getIsAmbient(), existing.doesShowParticles()));

        entity.setHealth(Math.min(health, entity.getMaxHealth()));
        if (potion == MobEffects.ABSORPTION) {
            absorption += 4.0F * (amplifier - oldAmplifier);
        }
        entity.setAbsorptionAmount(Math.max(0.0F, absorption));
    }

    private static void setDuration(EntityLivingBase entity, PotionEffect effect, int duration) {
        if (DURATION == null) {
            entity.addPotionEffect(new PotionEffect(effect.getPotion(), duration, effect.getAmplifier(),
                    effect.getIsAmbient(), effect.doesShowParticles()));
            return;
        }
        try {
            DURATION.setInt(effect, duration);
        } catch (IllegalAccessException e) {
            return;
        }
        // 服务端改了时长，客户端的倒计时要单独同步；其他玩家看不到这个数，不用管
        if (entity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entity).connection.sendPacket(new SPacketEntityEffect(entity.getEntityId(), effect));
        }
    }

    @Nullable
    private static Field findDurationField() {
        try {
            return ObfuscationReflectionHelper.findField(PotionEffect.class, "field_76460_b");
        } catch (RuntimeException e) {
            System.err.println("[EAddons] PotionEffect#duration not found, falling back to vanilla re-apply: " + e);
            return null;
        }
    }
}
