package net.mx.eaddons.client;

import net.minecraft.client.particle.ParticleSpell;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 深渊混沌粒子：混沌鞘翅加速时的拖尾。基于原版药水效果粒子改深紫色近似「深渊」。
 */
@SideOnly(Side.CLIENT)
public class ParticleAbyssChaos extends ParticleSpell {
    public ParticleAbyssChaos(World world, double x, double y, double z, double mx, double my, double mz) {
        super(world, x, y, z, mx, my, mz);
        this.particleRed = 0.32F;
        this.particleGreen = 0.05F;
        this.particleBlue = 0.45F;
        this.particleMaxAge = 12 + this.rand.nextInt(8);
        this.particleScale *= 1.2F;
    }
}
