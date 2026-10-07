package net.mx.eaddons.compat;

import goblinbob.mobends.core.util.BenderHelper;
import goblinbob.mobends.standard.data.PlayerData;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * MoBends 兼容渲染：在 MoBends 的动画身体骨骼上绘制混沌鞘翅翅膀。
 * 仅在 {@link ModCompat#MOBENDS} 为真时由调用方触及本类（避免未装 MoBends 时加载其 API）。
 * 做法对齐 MoBends 自己的 LayerCustomElytra：绑定到 data.body 后按 -12*scale 上移再渲染鞘翅模型。
 */
@SideOnly(Side.CLIENT)
public final class MoBendsElytraCompat {
    private MoBendsElytraCompat() {
    }

    /**
     * 若 MoBends 正在动画化该玩家，则在其身体骨骼上渲染鞘翅并返回 true；
     * 否则（未动画化）返回 false，交由调用方走原版渲染。
     */
    public static boolean renderElytra(RenderPlayer renderer, AbstractClientPlayer player, ModelElytra model,
                                       float limbSwing, float limbSwingAmount, float ageInTicks,
                                       float netHeadYaw, float headPitch, float scale) {
        PlayerData data = BenderHelper.getData(player, renderer);
        if (data == null) return false;

        GlStateManager.pushMatrix();
        data.body.applyCharacterTransform(0.0625F);
        GlStateManager.translate(0.0F, -12.0F * scale, 0.0F);
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);
        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();
        return true;
    }
}
