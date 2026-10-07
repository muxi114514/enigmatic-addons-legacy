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
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 超维封印载体的渲染器：把实体持有的超维之眼物品模型悬空、缓慢自转地绘制出来，
 * 观感与一枚漂浮的眼睛一致。因载体不再是 EntityItem，需自绘而非复用原版物品渲染器。
 */
@SideOnly(Side.CLIENT)
public class RenderExtradimensionalLock extends Render<EntityExtradimensionalLock> {
    private final RenderItem itemRenderer;

    public RenderExtradimensionalLock(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityExtradimensionalLock entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ItemStack stack = entity.getDisplayItem();
        if (stack.isEmpty()) return;

        // 上下轻微浮动 + 持续绕 Y 轴自转
        float age = entity.ticksExisted + partialTicks;
        float bob = MathHelper.sin(age * 0.1F) * 0.1F + 0.1F;
        float spin = age * 4.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + bob + 0.2F, z);
        GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.75F, 0.75F, 0.75F);

        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        this.itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityExtradimensionalLock entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
