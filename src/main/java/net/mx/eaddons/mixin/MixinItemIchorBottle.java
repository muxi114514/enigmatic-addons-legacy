package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.item.ItemIchorBottle;
import keletu.enigmaticlegacy.util.compat.ModCompat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.mx.eaddons.baubleslot.ElSlotUnlocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 灵液瓶同 {@link MixinItemAstralFruit}：没有 BaublesEX 时改走 BaubleVault 开项链槽 */
@Mixin(value = ItemIchorBottle.class, remap = false)
public class MixinItemIchorBottle {

    @Redirect(method = "func_77654_b(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;",
            at = @At(value = "FIELD", target = "Lkeletu/enigmaticlegacy/EnigmaticConfigs;maxIchorBottleSlotUnlocks:I",
                    opcode = Opcodes.GETSTATIC, remap = false), remap = false)
    private int eaddons$slotLimit() {
        return ModCompat.COMPAT_BAUBLES_EX ? EnigmaticConfigs.maxIchorBottleSlotUnlocks : 0;
    }

    @Inject(method = "func_77654_b(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"), remap = false)
    private void eaddons$unlockWithoutBaublesEx(ItemStack stack, World world, EntityLivingBase entity,
                                                CallbackInfoReturnable<ItemStack> cir) {
        if (entity instanceof EntityPlayerMP) {
            ElSlotUnlocks.grantIchorBottleSlot((EntityPlayerMP) entity);
        }
    }
}
