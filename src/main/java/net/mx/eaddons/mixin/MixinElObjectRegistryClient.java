package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 与 {@link MixinElObjectRegistry} 配套：给始终注册的天体果实、灵液瓶绑定模型。目标方法只存在于客户端 */
@Mixin(value = EnigmaticLegacy.ObjectRegistryHandler.class, remap = false)
public class MixinElObjectRegistryClient {

    @Redirect(method = "modelRegistryEvent(Lnet/minecraftforge/client/event/ModelRegistryEvent;)V",
            at = @At(value = "FIELD", target = "Lkeletu/enigmaticlegacy/util/compat/ModCompat;COMPAT_BAUBLES_EX:Z",
                    opcode = Opcodes.GETSTATIC, remap = false), remap = false)
    private static boolean eaddons$alwaysBindSlotItemModels() {
        return true;
    }
}
