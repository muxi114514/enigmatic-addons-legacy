package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.mx.eaddons.compat.ModCompat;

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
        event.setAmount(resist((EntityPlayer) event.getEntityLiving(), event.getSource(), event.getAmount()));
    }

    /**
     * 装了 First Aid 时在护甲结算前再减免一次。First Aid 按部位结算时也会发 LivingDamageEvent，
     * 所以这类玩家实际减免两次，当作特性保留（2026-10-04 用户确认）。
     * 这四种伤害本就无视护甲，前后结果一致（只有身上有伤害吸收时略有出入）。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!ModCompat.firstAidTakesOver(event.getEntityLiving())) return;
        event.setAmount(resist((EntityPlayer) event.getEntityLiving(), event.getSource(), event.getAmount()));
    }

    private static float resist(EntityPlayer player, DamageSource source, float amount) {
        if (!ItemQuartzRing.hasQuartzRing(player) || !MAGIC_DAMAGE_TYPES.contains(source.getDamageType())) {
            return amount;
        }
        return amount * (1.0F - ItemQuartzRing.MAGIC_RESISTANCE);
    }
}
