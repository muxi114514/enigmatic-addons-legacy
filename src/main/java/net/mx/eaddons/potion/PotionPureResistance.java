package net.mx.eaddons.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 纯化抗性（移植自 1.20 神遗拓展）：每级 20% 概率完全抵挡一次伤害，
 * 否则伤害 ×0.2×(4−等级)；V 级起必定抵挡。
 */
public class PotionPureResistance extends Potion {

    public static final PotionPureResistance INSTANCE = new PotionPureResistance();

    private final ResourceLocation icon = new ResourceLocation("eaddons:textures/mob_effect/pure_resistance.png");

    public PotionPureResistance() {
        super(false, 0xffbf4b);
        setRegistryName("pure_resistance");
        setPotionName("effect.pure_resistance");
        setBeneficial();
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        if (mc.currentScreen != null) {
            mc.getTextureManager().bindTexture(icon);
            Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(icon);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }

    /** 早于 First Aid 的按部位结算（LOWEST） */
    public static class Handler {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onLivingHurt(LivingHurtEvent event) {
            PotionEffect effect = event.getEntityLiving().getActivePotionEffect(INSTANCE);
            if (effect == null) {
                return;
            }
            int amplifier = effect.getAmplifier();
            if (event.getEntityLiving().getRNG().nextInt(5) <= amplifier) {
                event.setCanceled(true);
            } else {
                event.setAmount(event.getAmount() * 0.2F * (4 - amplifier));
            }
        }
    }
}
