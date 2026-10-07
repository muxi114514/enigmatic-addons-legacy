package net.mx.eaddons.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.entity.EntityAngelBeam;

/**
 * 天使光束渲染器：画一条从发射点连到当前位置的十字纹理光束（移植自 EL+ AngelBeamRenderer）。
 *
 * <p>1.12.2 用 {@link Tessellator} + {@link GlStateManager} 的旧管线：先把坐标系旋到光束方向，
 * 再画两个互相垂直的四边形，纹理沿长度方向平铺。拖尾粒子由实体自己在客户端发。
 */
@SideOnly(Side.CLIENT)
public class RenderAngelBeam extends Render<EntityAngelBeam> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("eaddons", "textures/entity/projectiles/angel_beam.png");
    private static final float WIDTH = 0.22F;

    public RenderAngelBeam(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityAngelBeam beam, double x, double y, double z, float entityYaw, float partialTicks) {
        // 当前渲染位置（插值）与发射起点的相对向量
        double curX = beam.lastTickPosX + (beam.posX - beam.lastTickPosX) * partialTicks;
        double curY = beam.lastTickPosY + (beam.posY - beam.lastTickPosY) * partialTicks;
        double curZ = beam.lastTickPosZ + (beam.posZ - beam.lastTickPosZ) * partialTicks;
        Vec3d begin = beam.getBeginning();
        double dx = begin.x - curX;
        double dy = begin.y - curY;
        double dz = begin.z - curZ;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4D) {
            return;   // 刚发射时起点≈当前位置，跳过以免归一化出 NaN
        }

        bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);

        // 把 +Z 轴旋到光束方向
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);

        float tail = (float) len;
        float repeat = tail;   // 纹理沿长度平铺，每格一次
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        // 两个互相垂直的面，构成十字光束
        quad(buffer, WIDTH, 0.0F, tail, repeat);
        quad(buffer, 0.0F, WIDTH, tail, repeat);
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
        super.doRender(beam, x, y, z, entityYaw, partialTicks);
    }

    /** 沿 +Z 方向铺一条长 len 的条带；halfW/halfH 二选一为 0，得到十字的一片。 */
    private static void quad(BufferBuilder buffer, float halfW, float halfH, float len, float repeat) {
        buffer.pos(-halfW, -halfH, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 220).endVertex();
        buffer.pos(halfW, halfH, 0.0D).tex(1.0D, 0.0D).color(255, 255, 255, 220).endVertex();
        buffer.pos(halfW, halfH, len).tex(1.0D, repeat).color(255, 255, 255, 60).endVertex();
        buffer.pos(-halfW, -halfH, len).tex(0.0D, repeat).color(255, 255, 255, 60).endVertex();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAngelBeam entity) {
        return TEXTURE;
    }
}
