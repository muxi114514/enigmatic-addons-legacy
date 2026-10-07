package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ItemHellBladeCharm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让地狱刃片护符为 EL 的诅咒层数贡献 +2。
 * <p>此前是往护符 NBT 里塞两个真实的诅咒附魔（消失诅咒 + 绑定诅咒）来凑数，但消失诅咒在原版的含义
 * 就是"死亡时销毁"，导致护符死后消失；且 HideFlags 把附魔行藏了，玩家无从得知。
 * <p>改为在此注入 {@code getCurseAmount(ItemStack)} 的返回值上加 2——与 EL 自己给七咒之戒
 * 硬编码 +7 的做法一致，也对齐 1.20 原版用 Mixin 上报诅咒数、物品本身不带附魔的设计。
 * <p>目标是模组类，运行期不做名称重映射，故 {@code remap = false}。
 */
@Mixin(value = SuperpositionHandler.class, remap = false)
public class MixinSuperpositionHandler {

    @Inject(method = "getCurseAmount(Lnet/minecraft/item/ItemStack;)I",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void eaddons$hellBladeCurseAmount(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack != null && !stack.isEmpty() && stack.getItem() == ItemHellBladeCharm.INSTANCE) {
            cir.setReturnValue(cir.getReturnValueI() + 2);
        }
    }
}
