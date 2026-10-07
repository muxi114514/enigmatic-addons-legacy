package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.item.etherium.EtheriumArmor;
import net.minecraft.entity.player.EntityPlayer;
import net.mx.eaddons.attribute.EtheriumShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 以太护盾判定改读 eaddons.etheriumShield 属性：原先写死「满套且生命 ≤ 40%」，
 * 现由以太套装、以太核心等以修饰符提供阈值。EL 的护盾结算（减伤、击退、免投射物）不变。
 */
@Mixin(value = EtheriumArmor.class, remap = false)
public class MixinEtheriumArmorShield {

    @Inject(method = "hasShield(Lnet/minecraft/entity/player/EntityPlayer;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$shieldFromAttribute(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(EtheriumShield.isActive(player));
    }
}
