package net.mx.eaddons.despair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** 绝境的事件入口：只把事件翻译成调用，逻辑都在 {@link DespairLastStand}。 */
public class LastStandEventHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP) {
            DespairLastStand.tick((EntityPlayerMP) event.player);
        }
    }

    /** LOWEST 且不接收已取消的事件：只认真正发生了的死亡。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote) {
            return;
        }
        if (victim instanceof EntityPlayerMP) {
            DespairLastStand.onDeath((EntityPlayerMP) victim);
        }
        Entity killer = event.getSource().getTrueSource();
        if (killer instanceof EntityPlayerMP && killer != victim && isHostileTo(victim, (EntityPlayer) killer)) {
            DespairLastStand.onHostileKill((EntityPlayerMP) killer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DespairLastStand.syncToClient((EntityPlayerMP) event.player);
        }
    }

    /** 怪物，或正把这名玩家当攻击目标的生物；打死路边的动物不算。 */
    private static boolean isHostileTo(EntityLivingBase victim, EntityPlayer player) {
        return victim instanceof IMob
                || victim instanceof EntityLiving && ((EntityLiving) victim).getAttackTarget() == player;
    }
}
