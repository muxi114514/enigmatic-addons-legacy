package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.effect.GrowingBloodlustEffect;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修 EL 血怒渐涨的扣血频率：原 isReady 恒为 true，变成每 tick 扣 1 血。
 * 按 1.20 原版改为每 HealthLossTicks/(1+等级) tick 扣 1 血。
 * <p>效果每 tick 被饕餮之锅重新施加、剩余时长一直被刷新，不能按剩余时长取模，改按实体存活 tick 计时。
 */
@Mixin(value = GrowingBloodlustEffect.class, remap = false)
public class MixinGrowingBloodlustEffect {

    @Inject(method = "func_76394_a(Lnet/minecraft/entity/EntityLivingBase;I)V", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void eaddons$throttleHealthLoss(EntityLivingBase living, int amplifier, CallbackInfo ci) {
        int period = Math.max(1, EnigmaticConfigs.bloodLustHealthLossTicks / (1 + amplifier));
        if (living.ticksExisted % period != 0) {
            ci.cancel();
        }
    }
}
