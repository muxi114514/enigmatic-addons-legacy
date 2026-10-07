package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 天体果实与灵液瓶原本只在装了 BaublesEX 时才注册，这里让它们始终注册。
 * <p>只改注册处这一个判定，不能改 COMPAT_BAUBLES_EX 本身——EL 别处会据此直接调用 BaublesEX 的接口。
 * 已离线验证：原版 Baubles 下加载并初始化这两个物品类不会去加载 BaublesEX 的类。
 */
@Mixin(value = EnigmaticLegacy.ObjectRegistryHandler.class, remap = false)
public class MixinElObjectRegistry {

    @Redirect(method = "addItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V",
            at = @At(value = "FIELD", target = "Lkeletu/enigmaticlegacy/util/compat/ModCompat;COMPAT_BAUBLES_EX:Z",
                    opcode = Opcodes.GETSTATIC, remap = false), remap = false)
    private static boolean eaddons$alwaysRegisterSlotItems() {
        return true;
    }
}
