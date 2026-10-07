package net.mx.eaddons.mixin;

import keletu.enigmaticlegacy.item.etherium.EtheriumArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ArmorConfig;
import net.mx.eaddons.item.ArmorTextureHelper;
import net.mx.eaddons.item.ItemEvilArmor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对神秘遗物以太护甲的两处注入：
 * <ol>
 *   <li><b>套装判定放行极恶护甲</b>：EL 的 {@code hasFullSet} 用的是
 *   {@code stack.getItem().getClass() == EtheriumArmor.class} 这种严格类相等判定，任何外部护甲
 *   （哪怕是子类）都会被判否。改写为「每个甲位是以太或极恶护甲即可」，于是极恶套装无需复制任何逻辑，
 *   就自动获得 EL 的以太护盾、弹射物反弹、击退与音效；两族混穿同样成立。</li>
 *   <li><b>让以太护甲可见</b>：EL 固定返回全透明的 {@code unseen_armor.png}，此处改为返回本模组的
 *   以太色动画贴图，与极恶套装构成同款异色的一系列。可由配置关闭以恢复隐身。</li>
 * </ol>
 * 目标为模组类，运行期不做名称重映射，故 {@code remap = false}。
 */
@Mixin(value = EtheriumArmor.class, remap = false)
public class MixinEtheriumArmor {

    @Inject(method = "hasFullSet(Lnet/minecraft/entity/player/EntityPlayer;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$acceptEvilArmor(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) {
            cir.setReturnValue(false);
            return;
        }
        for (ItemStack stack : player.getArmorInventoryList()) {
            if (stack == null || stack.isEmpty()) {
                cir.setReturnValue(false);
                return;
            }
            boolean etherium = stack.getItem().getClass() == EtheriumArmor.class;
            if (!etherium && !ItemEvilArmor.is(stack)) {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "getArmorTexture(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;"
            + "Lnet/minecraft/inventory/EntityEquipmentSlot;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$visibleEtheriumTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot,
                                                String type, CallbackInfoReturnable<String> cir) {
        if (!ArmorConfig.visibleEtheriumArmor) {
            return;
        }
        long time = entity != null && entity.world != null ? entity.world.getTotalWorldTime() : 0L;
        cir.setReturnValue(ArmorTextureHelper.getTexture(ArmorTextureHelper.SET_ETHERIUM, slot, time));
    }
}
