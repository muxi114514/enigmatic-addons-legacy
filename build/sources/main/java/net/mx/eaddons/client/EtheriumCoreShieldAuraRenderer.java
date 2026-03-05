package net.mx.eaddons.client;

import net.mx.eaddons.item.EtheriumCoreClientHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class EtheriumCoreShieldAuraRenderer {

    private static final Random RAND = new Random();
    /** Particles per tick when shield is active (spread over ~1.5 blocks around player). */
    private static final int PARTICLES_PER_TICK = 2;
    private static final double HORIZ_RADIUS = 0.6;
    private static final double VERT_OFFSET = 0.2;
    private static final double VERT_SPAN = 1.4;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EtheriumCoreClientHelper.tickShield();
            spawnShieldParticles();
        }
    }

    /** Spawn enchantment-table rune particles around the local player when shield is active. */
    private void spawnShieldParticles() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (player == null || world == null || !EtheriumCoreClientHelper.hasShieldActive(player)) return;

        double px = player.posX;
        double py = player.posY;
        double pz = player.posZ;
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double dx = (RAND.nextDouble() - 0.5) * 2 * HORIZ_RADIUS;
            double dy = VERT_OFFSET + RAND.nextDouble() * VERT_SPAN;
            double dz = (RAND.nextDouble() - 0.5) * 2 * HORIZ_RADIUS;
            double mx = (RAND.nextDouble() - 0.5) * 0.02;
            double my = 0.02 + RAND.nextDouble() * 0.02;
            double mz = (RAND.nextDouble() - 0.5) * 0.02;
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    px + dx, py + dy, pz + dz,
                    mx, my, mz);
        }
    }
}
