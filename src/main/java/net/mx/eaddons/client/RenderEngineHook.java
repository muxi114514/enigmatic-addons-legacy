package net.mx.eaddons.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.entity.EntityEngineHook;

/**
 * 抓钩渲染器：钩体按飞行朝向画一个十字片，再从持有者手心拉一条链条纹理到钩上。
 *
 * <p>1.20.1 那边有现成的 {@code EngineHookRenderer}，1.12.2 没有对应基建，
 * 链条这段参考原版 {@code RenderFish} 画鱼线的思路：取持有者手部的插值位置，
 * 分段铺一条始终面向摄像机的条带。
 */
@SideOnly(Side.CLIENT)
public class RenderEngineHook extends Render<EntityEngineHook> {

    private static final ResourceLocation HOOK_TEXTURE =
            new ResourceLocation("eaddons", "textures/entity/projectiles/engine_hook.png");
    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation("eaddons", "textures/entity/projectiles/engine_chain.png");

    public RenderEngineHook(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityEngineHook hook, double x, double y, double z, float entityYaw, float partialTicks) {
        renderHook(hook, x, y, z, partialTicks);
        renderChain(hook, x, y, z, partialTicks);
        super.doRender(hook, x, y, z, entityYaw, partialTicks);
    }

    /** 钩体：两片互相垂直的方片，按飞行朝向摆放。 */
    private void renderHook(EntityEngineHook hook, double x, double y, double z, float partialTicks) {
        bindTexture(HOOK_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        float yaw = hook.prevRotationYaw + (hook.rotationYaw - hook.prevRotationYaw) * partialTicks;
        float pitch = hook.prevRotationPitch + (hook.rotationPitch - hook.prevRotationPitch) * partialTicks;
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        final float s = 0.2F;
        // 竖片
        buffer.pos(-s, -s, 0.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos(s, -s, 0.0D).tex(1.0D, 1.0D).endVertex();
        buffer.pos(s, s, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(-s, s, 0.0D).tex(0.0D, 0.0D).endVertex();
        // 横片
        buffer.pos(0.0D, -s, -s).tex(0.0D, 1.0D).endVertex();
        buffer.pos(0.0D, -s, s).tex(1.0D, 1.0D).endVertex();
        buffer.pos(0.0D, s, s).tex(1.0D, 0.0D).endVertex();
        buffer.pos(0.0D, s, -s).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /** 链条：从持有者手心拉到钩体，分段铺条带。 */
    private void renderChain(EntityEngineHook hook, double x, double y, double z, float partialTicks) {
        Entity owner = hook.getOwner();
        if (!(owner instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) owner;

        // 手心位置：从持有者身侧按朝向偏移，取插值避免抖动
        double yaw = Math.toRadians(living.prevRenderYawOffset
                + (living.renderYawOffset - living.prevRenderYawOffset) * partialTicks);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double side = living == net.minecraft.client.Minecraft.getMinecraft().player ? 0.0D : 0.35D;
        double ox = living.lastTickPosX + (living.posX - living.lastTickPosX) * partialTicks - cos * side - sin * 0.8D;
        double oy = living.lastTickPosY + (living.posY - living.lastTickPosY) * partialTicks
                + living.getEyeHeight() - 0.35D;
        double oz = living.lastTickPosZ + (living.posZ - living.lastTickPosZ) * partialTicks - sin * side + cos * 0.8D;

        double hx = hook.lastTickPosX + (hook.posX - hook.lastTickPosX) * partialTicks;
        double hy = hook.lastTickPosY + (hook.posY - hook.lastTickPosY) * partialTicks;
        double hz = hook.lastTickPosZ + (hook.posZ - hook.lastTickPosZ) * partialTicks;

        bindTexture(CHAIN_TEXTURE);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 绳子绕障后是一串折线：钩子 → 各转折点 → 玩家手心，逐段画
        java.util.List<net.minecraft.util.math.Vec3d> path = new java.util.ArrayList<>();
        path.add(new net.minecraft.util.math.Vec3d(hx, hy, hz));
        path.addAll(EngineRopeController.ropeFor(hook).getPoints());
        path.add(new net.minecraft.util.math.Vec3d(ox, oy, oz));
        for (int i = 0; i < path.size() - 1; i++) {
            net.minecraft.util.math.Vec3d from = path.get(i);
            net.minecraft.util.math.Vec3d to = path.get(i + 1);
            // 每段都相对钩子位置平移，好复用外层传进来的 x/y/z
            renderSegment(x + (from.x - hx), y + (from.y - hy), z + (from.z - hz),
                    to.x - from.x, to.y - from.y, to.z - from.z);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    /** 画一段链条：从给定起点出发，沿 (dx,dy,dz) 铺一条十字条带。 */
    private static void renderSegment(double x, double y, double z, double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4D) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        float chainYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float chainPitch = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        GlStateManager.rotate(chainYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-chainPitch, 1.0F, 0.0F, 0.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        final float w = 0.05F;
        float repeat = (float) len * 2.0F;   // 链节沿长度重复
        buffer.pos(-w, 0.0D, 0.0D).tex(0.0D, 0.0D).endVertex();
        buffer.pos(w, 0.0D, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(w, 0.0D, len).tex(1.0D, repeat).endVertex();
        buffer.pos(-w, 0.0D, len).tex(0.0D, repeat).endVertex();
        buffer.pos(0.0D, -w, 0.0D).tex(0.0D, 0.0D).endVertex();
        buffer.pos(0.0D, w, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(0.0D, w, len).tex(1.0D, repeat).endVertex();
        buffer.pos(0.0D, -w, len).tex(0.0D, repeat).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityEngineHook entity) {
        return HOOK_TEXTURE;
    }
}
