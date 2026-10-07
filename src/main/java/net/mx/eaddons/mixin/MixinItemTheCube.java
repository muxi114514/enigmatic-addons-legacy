package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.item.ItemTheCube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.mx.eaddons.primeval.PrimevalConfig;
import net.mx.eaddons.primeval.TheCubeRework;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 非欧立方主动改版：原主动「随机传送到另外两个主维度之一」整个换成 {@link TheCubeRework}，
 * 冷却也改读本模组配置——tooltip 的冷却秒数取自 getCooldown，一并对上。
 * <p>目标是模组类，运行期不做名称重映射，故 {@code remap = false}。
 */
@Mixin(value = ItemTheCube.class, remap = false)
public class MixinItemTheCube {

    @Inject(method = "triggerActiveAbility", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$reworkActive(World world, EntityPlayerMP player, ItemStack stack, CallbackInfo ci) {
        TheCubeRework.activate(player);
        ci.cancel();
    }

    @Inject(method = "getCooldown", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$cooldown(EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(PrimevalConfig.cubeCooldown);
    }
}
