package net.mx.eaddons.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.mx.eaddons.despair.DespairLastStand;
import net.mx.eaddons.item.SpelltunerDeathWard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版图腾保命检查 {@code checkTotemDeathProtection} 上的免死，First Aid 判定玩家死亡时也会调这个方法：
 * <ul>
 * <li>开头：绝望者证章的绝境进行中则锁血（不消耗图腾），否则轮到虚空调谐按概率免死</li>
 * <li>返回处：图腾也没救下时，绝望者证章触发绝境</li>
 * </ul>
 *
 * <p>方法名写 SRG（{@code func_190628_d}）并 {@code remap = false}，理由同 {@link MixinEntityIsWet}。
 * 优先级 500（默认 1000）：同一处 HEAD 回调按应用顺序执行，保证锁血与虚空调谐先于
 * SoManyEnchantments 的复活符文（一次性、会被消耗）判定。
 */
@Mixin(value = EntityLivingBase.class, priority = 500)
public class MixinEntityLivingTotem {

    @Inject(method = "func_190628_d", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$beforeTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (DespairLastStand.holdAtDeath(self, source) || SpelltunerDeathWard.tryWard(self, source)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "func_190628_d", at = @At("RETURN"), cancellable = true, remap = false)
    private void eaddons$afterTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && DespairLastStand.tryTrigger((EntityLivingBase) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }
}
