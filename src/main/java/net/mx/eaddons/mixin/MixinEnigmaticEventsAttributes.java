package net.mx.eaddons.mixin;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.event.EnigmaticEvents;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 停用 EL 里已改由属性承载的写死效果，结算改在 {@code net.mx.eaddons.attribute}，避免重复生效。
 * <ul>
 *   <li>吸血：护符黑、邪术护符、无止之言、饕餮之锅、血怒全部汇总到一次 heal，只拦这一次；锅的偷饱食保留</li>
 *   <li>投射物偏转、挖掘加成、暴击加成：处理方法里的来源已全部迁走，整段跳过</li>
 *   <li>品红护符（缓降 / 跳跃 / 摔伤免疫）与失落引擎下坠：只屏蔽对应判定，海洋之石、失落引擎冲刺等照常</li>
 * </ul>
 * 目标为模组类，MC 方法写 SRG 名，{@code remap = false}。已核对 2.8.0 与 2.9.0 两版字节码一致。
 */
@Mixin(value = EnigmaticEvents.class, remap = false)
public class MixinEnigmaticEventsAttributes {

    @Redirect(method = "onEntityDamaged(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;func_70691_i(F)V",
                    ordinal = 0, remap = false), remap = false)
    private static void eaddons$skipLifestealHeal(EntityPlayer player, float amount) {
        // 吸血改由 eaddons.lifesteal 属性结算
    }

    @Inject(method = "onProjectileImpact(Lnet/minecraftforge/event/entity/ProjectileImpactEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$skipDeflect(ProjectileImpactEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "miningStuff(Lnet/minecraftforge/event/entity/player/PlayerEvent$BreakSpeed;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$skipMiningBoost(PlayerEvent.BreakSpeed event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onCriticalHit(Lnet/minecraftforge/event/entity/player/CriticalHitEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void eaddons$skipCritBoost(CriticalHitEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = {
            "onEntityUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
            "onEntityJump(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingJumpEvent;)V",
            "onEntityHurt(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V"},
            at = @At(value = "INVOKE",
                    target = "Lkeletu/enigmaticlegacy/event/SuperpositionHandler;isWearEnigmaticAmulet(Lnet/minecraft/entity/player/EntityPlayer;I)Z",
                    remap = false), remap = false)
    private static boolean eaddons$skipMagenta(EntityPlayer player, int color) {
        return color != 4 && SuperpositionHandler.isWearEnigmaticAmulet(player, color);
    }

    @Redirect(method = "onEntityUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
            at = @At(value = "INVOKE",
                    target = "Lbaubles/api/BaublesApi;isBaubleEquipped(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/Item;)I",
                    remap = false), remap = false)
    private static int eaddons$skipLostEngineFall(EntityPlayer player, Item item) {
        return item == EnigmaticLegacy.lostEngine ? -1 : BaublesApi.isBaubleEquipped(player, item);
    }
}
