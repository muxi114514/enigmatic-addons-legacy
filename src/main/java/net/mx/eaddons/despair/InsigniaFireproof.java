package net.mx.eaddons.despair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.mx.eaddons.item.ItemInsigniaOfDespair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * 绝望者证章的掉落物不怕火与岩浆（同 1.20.1 原版的 fireResistant）。
 * 掉落物生成、区块载入时都会发 EntityJoinWorldEvent，每次重新置位即可，掉落物仍是原版实体、照常存档。
 */
public class InsigniaFireproof {
    private static final Logger LOG = LogManager.getLogger("eaddons");
    /** Entity.isImmuneToFire */
    private static Field immuneToFire;
    private static boolean unavailable;

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (unavailable || event.getWorld().isRemote || !(entity instanceof EntityItem)
                || ((EntityItem) entity).getItem().getItem() != ItemInsigniaOfDespair.INSTANCE) {
            return;
        }
        try {
            if (immuneToFire == null) {
                immuneToFire = ReflectionHelper.findField(Entity.class, "field_70178_ae", "isImmuneToFire");
            }
            immuneToFire.setBoolean(entity, true);
        } catch (RuntimeException | IllegalAccessException e) {
            unavailable = true;
            LOG.error("[Insignia] Could not make the dropped insignia fireproof", e);
        }
    }
}
