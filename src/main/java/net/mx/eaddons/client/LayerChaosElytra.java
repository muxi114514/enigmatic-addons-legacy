package net.mx.eaddons.client;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.compat.MoBendsElytraCompat;
import net.mx.eaddons.compat.ModCompat;
import net.mx.eaddons.item.ChaosElytraHelper;

/**
 * 混沌鞘翅的翅膀渲染层：当玩家佩戴混沌鞘翅时，在其背部渲染鞘翅模型（自定义贴图）。
 * 移植自 1.20 的 ChaosElytraLayer，用 1.12.2 的 ModelElytra + LayerRenderer 实现。
 */
@SideOnly(Side.CLIENT)
public class LayerChaosElytra implements LayerRenderer<EntityLivingBase> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("eaddons", "textures/entity/chaos_elytra.png");

    private final net.minecraft.client.renderer.entity.RenderPlayer renderer;
    private final ModelElytra modelElytra = new ModelElytra();

    public LayerChaosElytra(net.minecraft.client.renderer.entity.RenderPlayer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!(entity instanceof EntityPlayer)) return;
        // 仅在佩戴混沌鞘翅时渲染翅膀
        if (ChaosElytraHelper.getChaosElytra((EntityPlayer) entity).isEmpty()) return;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        this.renderer.bindTexture(TEXTURE);

        // MoBends 装载时：在其动画身体骨骼空间绘制，翅膀随动画贴合后背（未动画化则回退原版坐标）
        boolean rendered = false;
        if (ModCompat.MOBENDS && entity instanceof AbstractClientPlayer) {
            rendered = MoBendsElytraCompat.renderElytra(this.renderer, (AbstractClientPlayer) entity, this.modelElytra,
                    limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        if (!rendered) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.125F);
            this.modelElytra.setRotationAngles(limbSwing, limbSwingAmount, partialTicks, netHeadYaw, headPitch, scale, entity);
            this.modelElytra.render(entity, limbSwing, limbSwingAmount, partialTicks, netHeadYaw, headPitch, scale);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
