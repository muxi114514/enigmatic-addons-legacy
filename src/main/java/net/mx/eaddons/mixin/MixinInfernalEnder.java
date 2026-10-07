package net.mx.eaddons.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.mx.eaddons.spellstone.CubeResonance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 非欧领域对精英怪「Ender」词条的拦截。
 *
 * <p>原版末影人与潜影贝的瞬移都会发 Forge 的 {@code EnderTeleportEvent}，取消即可；
 * Infernal Mobs 是自己写的 {@code teleportTo}，不发任何事件，只能从这里返回 false。
 * 该词条原本是「挨打时瞬移走、本次伤害归零并反弹给攻击者」，拦下之后它会正常吃伤害。
 *
 * <p>目标用字符串写、配 {@code @Pseudo} 与 required=false：没装精英怪时整份配置静默跳过，
 * 编译期也不需要它的 jar。
 */
@Pseudo
@Mixin(targets = "atomicstryker.infernalmobs.common.mods.MM_Ender", remap = false)
public class MixinInfernalEnder {

    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$blockInDomain(EntityLivingBase mob, double x, double y, double z,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (CubeResonance.domainBlocks(mob)) {
            cir.setReturnValue(false);
        }
    }
}
