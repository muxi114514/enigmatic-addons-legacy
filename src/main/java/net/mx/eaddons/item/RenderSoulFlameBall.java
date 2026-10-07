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
 * 灵魂火球渲染器：将火球持有的 soul_flame_ball 物品作为面向摄像机的公告板绘制，
 * 等价于 1.20 的 ThrownItemRenderer。粒子拖尾另由实体自身在客户端生成。
 */
@SideOnly(Side.CLIENT)
public class RenderSoulFlameBall extends Render<EntitySoulFlameBall> {
    private final RenderItem itemRenderer;
    private final ItemStack ballStack;

    public RenderSoulFlameBall(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
        this.ballStack = new ItemStack(ItemSoulFlameBall.INSTANCE);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntitySoulFlameBall entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height * 0.5F, z);
        // 公告板：抵消摄像机的偏航与俯仰，使火球始终面向玩家
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.75F, 0.75F, 0.75F);

        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        this.itemRenderer.renderItem(this.ballStack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySoulFlameBall entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
