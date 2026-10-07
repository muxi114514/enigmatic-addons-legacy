package net.mx.eaddons.client;

import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 灵魂粒子：1.12.2 没有 SOUL 粒子，基于原版烟雾粒子改青色 + 缓慢上升近似。
 */
@SideOnly(Side.CLIENT)
public class ParticleSoul extends ParticleSmokeNormal {
    public ParticleSoul(World world, double x, double y, double z, double mx, double my, double mz) {
        super(world, x, y, z, mx, my, mz, 1.0F);
        this.particleRed = 0.22F;
        this.particleGreen = 0.78F;
        this.particleBlue = 0.90F;
        this.motionY += 0.015; // 灵魂缓缓上浮
        this.particleMaxAge = 20 + this.rand.nextInt(10);
    }
}
