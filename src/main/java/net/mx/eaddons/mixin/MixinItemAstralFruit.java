package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.item.ItemAstralFruit;
import keletu.enigmaticlegacy.util.compat.ModCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.mx.eaddons.baubleslot.ElSlotUnlocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 天体果实在没有 BaublesEX 时：上限读成 0，跳过 EL 那段 BaublesEX 开槽代码（药水照给），
 * 改由 {@link ElSlotUnlocks} 走 BaubleVault 开戒指槽；两者都没有就只给药水。
 */
@Mixin(value = ItemAstralFruit.class, remap = false)
public class MixinItemAstralFruit {

    @Redirect(method = "func_77849_c(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)V",
            at = @At(value = "FIELD", target = "Lkeletu/enigmaticlegacy/EnigmaticConfigs;maxAstralFruitSlotUnlocks:I",
                    opcode = Opcodes.GETSTATIC, remap = false), remap = false)
    private int eaddons$slotLimit() {
        return ModCompat.COMPAT_BAUBLES_EX ? EnigmaticConfigs.maxAstralFruitSlotUnlocks : 0;
    }

    @Inject(method = "func_77849_c(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)V",
            at = @At("HEAD"), remap = false)
    private void eaddons$unlockWithoutBaublesEx(ItemStack stack, World world, EntityPlayer player, CallbackInfo ci) {
        if (player instanceof EntityPlayerMP) {
            ElSlotUnlocks.grantAstralFruitSlot((EntityPlayerMP) player);
        }
    }
}
