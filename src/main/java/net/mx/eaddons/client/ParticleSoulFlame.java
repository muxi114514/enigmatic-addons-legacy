package net.mx.eaddons.client;

import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 灵魂火焰粒子：1.12.2 没有 SOUL_FIRE_FLAME，基于原版火焰粒子改青色近似灵魂火。
 * 原版 {@link ParticleFlame} 仅在构造时设定颜色、渲染时只改缩放，故构造后重设颜色即可持久生效。
 */
@SideOnly(Side.CLIENT)
public class ParticleSoulFlame extends ParticleFlame {
    public ParticleSoulFlame(World world, double x, double y, double z, double mx, double my, double mz) {
        super(world, x, y, z, mx, my, mz);
        this.particleRed = 0.28F;
        this.particleGreen = 0.82F;
        this.particleBlue = 0.98F;
    }
}
