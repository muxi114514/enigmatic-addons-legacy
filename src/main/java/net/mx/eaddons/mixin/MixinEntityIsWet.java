package net.mx.eaddons.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneSwordConfig;
import net.mx.eaddons.spellstone.SpellstoneData;
import net.mx.eaddons.spellstone.SpellstoneForm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 海洋形态满级：让持剑者恒为「湿润」。
 *
 * <p>选 {@code isWet()} 而不是 {@code isInWater()}：后者参与移动逻辑，改了会让玩家在陆地上游泳；
 * 前者只是一个「湿没湿」的查询，原版只用它判熄火与雪人受伤，冰与火的海蛇鳞甲（潮卫套）
 * 套装效果判的也正是它（{@code EventLiving:440}）。一处改动同时喂饱自家被动和潮卫套。
 *
 * <p>已知副作用：{@code Entity#onEntityUpdate} 里 {@code if (isWet()) extinguish()}，
 * 满级后佩戴者永远无法被点燃——这符合「永远在水中」的设定，已写进 tooltip。
 *
 * <p>方法名写 SRG（{@code func_70026_G}）并 {@code remap = false}：本项目没有配 Mixin 注解处理器、
 * 不生成 refmap，运行期是 SRG 环境，写 MCP 名会匹配不上。代价是开发环境 runClient 里这条不生效。
 */
@Mixin(Entity.class)
public class MixinEntityIsWet {

    @Inject(method = "func_70026_G", at = @At("HEAD"), cancellable = true, remap = false)
    private void eaddons$oceanAlwaysWet(CallbackInfoReturnable<Boolean> cir) {
        if (!SpellstoneSwordConfig.oceanMaxLevelAlwaysWet || !((Object) this instanceof EntityPlayer)) {
            return;
        }
        ItemStack held = ((EntityPlayer) (Object) this).getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemSpellstoneSword)) {
            return;
        }
        // 原初共鸣常驻十形态的持握被动，海洋这条也在其中，所以任何子形态下都恒湿润
        if (SpellstoneData.isPrimevalActive(held)
                || (SpellstoneData.isResonatingWith(held, SpellstoneForm.OCEAN_STONE)
                        && SpellstoneData.isMaxLevel(held))) {
            cir.setReturnValue(true);
        }
    }
}
