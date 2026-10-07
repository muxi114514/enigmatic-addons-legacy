package net.mx.eaddons.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 魔法石英匕首投射物渲染器：沿飞行方向摆放并渲染 3D 匕首物品模型，
 * 移植自 1.20 神遗拓展 AbstractSpearRenderer 的朝向与缩放逻辑，用 1.12.2 的固定管线实现。
 */
@SideOnly(Side.CLIENT)
public class RenderQuartzDagger extends Render<EntityQuartzDagger> {
    private static final float SCALE = 0.68F;

    private final RenderItem itemRenderer;
    private final ItemStack daggerStack;

    public RenderQuartzDagger(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
        this.daggerStack = new ItemStack(ItemQuartzDagger.INSTANCE);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityQuartzDagger entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // 与 1.20 AbstractSpearRenderer 一致：先按插值后的偏航/俯仰摆正模型
        // 1.12.2 无 MathHelper.lerp，手动线性插值
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90.0F;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks - 45.0F;
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        this.itemRenderer.renderItem(this.daggerStack, ItemCameraTransforms.TransformType.NONE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityQuartzDagger entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
