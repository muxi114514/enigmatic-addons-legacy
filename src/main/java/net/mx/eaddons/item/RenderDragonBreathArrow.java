package net.mx.eaddons.item;

import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderDragonBreathArrow extends RenderArrow<EntityDragonBreathArrow> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("eaddons",
            "textures/entity/projectiles/dragon_breath_arrow.png");

    public RenderDragonBreathArrow(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDragonBreathArrow entity) {
        return TEXTURE;
    }
}
