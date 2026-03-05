package net.mx.eaddons.potion;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.util.ResourceLocation;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.Minecraft;

/**
 * Ichor Corrosion potion effect, ported from Enigmatic Addons.
 * Per level: -20% armor, -20% armor toughness, -20% knockback resistance.
 * The +10% damage taken per level is handled in TheBlessEventHandler.
 */
public class PotionIchorCorrosion extends Potion {
    public static final PotionIchorCorrosion INSTANCE = new PotionIchorCorrosion();

    private static final String ARMOR_UUID = "56ea94eb-c5ff-43f6-b4a2-c98b3390b279";
    private static final String ARMOR_TOUGHNESS_UUID = "a5e421c9-d83a-48d4-a4c9-2f72848419ec";
    private static final String KNOCKBACK_UUID = "89fee955-c75e-4eeb-b4c2-80c4cc636a24";

    private final ResourceLocation potionIcon;

    public PotionIchorCorrosion() {
        super(true, 0xce753d);
        setRegistryName("ichor_corrosion");
        setPotionName("effect.ichor_corrosion");
        potionIcon = new ResourceLocation("eaddons:textures/mob_effect/ichor_corrosion.png");

        registerPotionAttributeModifier(SharedMonsterAttributes.ARMOR,
                ARMOR_UUID, -0.2D, 2);
        registerPotionAttributeModifier(SharedMonsterAttributes.ARMOR_TOUGHNESS,
                ARMOR_TOUGHNESS_UUID, -0.2D, 2);
        registerPotionAttributeModifier(SharedMonsterAttributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_UUID, -0.2D, 2);
    }

    @Override
    public boolean isInstant() {
        return false;
    }

    @Override
    public boolean shouldRenderInvText(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderHUD(PotionEffect effect) {
        return true;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        if (mc.currentScreen != null) {
            mc.getTextureManager().bindTexture(potionIcon);
            Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(potionIcon);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}
