package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuartzRingEventHandler {

    private static final Set<String> MAGIC_DAMAGE_TYPES = new HashSet<>(Arrays.asList(
            "magic", "indirectMagic", "wither", "dragonBreath"
    ));

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!ItemQuartzRing.hasQuartzRing(player)) return;

        if (MAGIC_DAMAGE_TYPES.contains(event.getSource().getDamageType())) {
            event.setAmount(event.getAmount() * (1.0F - ItemQuartzRing.MAGIC_RESISTANCE));
        }
    }
}
