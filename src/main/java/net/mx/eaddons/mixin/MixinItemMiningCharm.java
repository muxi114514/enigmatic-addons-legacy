package net.mx.eaddons.mixin;

import baubles.api.BaubleType;
import keletu.enigmaticlegacy.item.ItemMiningCharm;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.miningcharm.MiningCharmConfig;
import net.mx.eaddons.miningcharm.MiningCharmNightVision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 猎宝者护符：改为万能饰品（任意饰品格可放），以及永久夜视（逻辑见 {@link MiningCharmNightVision}）。
 */
@Mixin(value = ItemMiningCharm.class, remap = false)
public class MixinItemMiningCharm {

    /** EL 写死为挂饰；同时只能戴一个仍由 EL 自己的 canEquip 保证。 */
    @Inject(method = "getBaubleType", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$anySlot(ItemStack stack, CallbackInfoReturnable<BaubleType> cir) {
        if (MiningCharmConfig.anySlot) {
            cir.setReturnValue(BaubleType.TRINKET);
        }
    }

    /** 注入在 super.onWornTick（父类的饰品 tick）之后，只替换夜视那段条件判断。 */
    @Inject(method = "onWornTick", cancellable = true, remap = false,
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false,
                    target = "Lkeletu/enigmaticlegacy/item/ItemBaseBauble;onWornTick(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V"))
    private void eaddons$permanentNightVision(ItemStack stack, EntityLivingBase living, CallbackInfo ci) {
        if (MiningCharmNightVision.tick((ItemMiningCharm) (Object) this, stack, living)) {
            ci.cancel();
        }
    }
}
