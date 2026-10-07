package net.mx.eaddons.mixin;

import baubles.api.BaublesApi;
import com.github.alexthe666.iceandfire.api.IEntityEffectCapability;
import com.github.alexthe666.iceandfire.capability.entityeffect.EntityEffectHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.mx.eaddons.item.FrostHelper;
import net.mx.eaddons.item.ItemForgottenIce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 忘却冰晶与冰与火冻结的两处接缝。目标是软依赖模组，配置 {@code mixins.eaddons.iceandfire.json}
 * 设了 required=false，缺冰与火时整份 mixin 静默跳过。
 *
 * <p>为什么打在 {@code tickUpdate} 而不是 {@code EntityEffectCapability#setEffect}：
 * 后者是纯数据类，字段里没有实体引用，拿不到「谁被冻了」，无从判断佩戴。
 * 而冻结的全部效果（减速、视角锁、severity≥2 的掉血）都在 tickUpdate 内结算，
 * 在它入口拦截等于一次都不生效，是真正的阻断层，而非「先中招再清除」。
 */
@Pseudo
@Mixin(value = EntityEffectHandler.class, remap = false)
public class MixinIafEntityEffectHandler {

    /** 佩戴忘却冰晶时免疫冰与火的冻结（冰龙冰息、冰晶爆炸等）。 */
    @Inject(method = "tickUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$immuneFrost(EntityLivingBase entity, World world,
                                            IEntityEffectCapability cap, CallbackInfo ci) {
        if (cap == null || !cap.isFrozen() || FrostHelper.hasFrost(entity)) {
            return;
        }
        if (entity instanceof EntityPlayer
                && BaublesApi.isBaubleEquipped((EntityPlayer) entity, ItemForgottenIce.INSTANCE) != -1) {
            cap.reset();
            ci.cancel();
        }
    }

    /**
     * 让本模组施加的冻结不被火焰解除。
     *
     * <p>冰与火的冻结分支在目标燃烧且 severity 为 0 时会 extinguish + reset，冰壳随之消失。
     * 本模组每 tick 借它画冰壳（severity 0），若不拦这个 reset，被点燃的目标冰壳会持续闪烁。
     * 这里只拦冻结分支里的那一个 reset——用 slice 圈定 isFrozen 到 isBlazed 之间，
     * 比写死 ordinal 抗版本漂移。
     */
    @Redirect(method = "tickUpdate",
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lcom/github/alexthe666/iceandfire/api/IEntityEffectCapability;isFrozen()Z"),
                    to = @At(value = "INVOKE",
                            target = "Lcom/github/alexthe666/iceandfire/api/IEntityEffectCapability;isBlazed()Z")),
            at = @At(value = "INVOKE",
                    target = "Lcom/github/alexthe666/iceandfire/api/IEntityEffectCapability;reset()V"),
            remap = false)
    private static void eaddons$keepFrost(IEntityEffectCapability receiver, EntityLivingBase entity,
                                          World world, IEntityEffectCapability cap) {
        if (!FrostHelper.hasFrost(entity)) {
            receiver.reset();
        }
    }
}
