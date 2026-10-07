package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.event.EnigmaticEvents;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.mx.eaddons.despair.DespairLastStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 绝境失败的死亡不可挽回：跳过 EL 死亡事件里的免死（非欧立方的锁血与复活、虚空珍珠、无止之言）。
 * {@code require = 0}：EL 改了方法名时只是这条失效，不致崩游戏。
 */
@Mixin(value = EnigmaticEvents.class, remap = false)
public class MixinEnigmaticEvents {

    @Inject(method = {"onLivingDeath", "onDeathLow"}, at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private static void eaddons$despairIsFinal(LivingDeathEvent event, CallbackInfo ci) {
        if (DespairLastStand.isDespairDeath(event.getSource())) {
            ci.cancel();
        }
    }
}
