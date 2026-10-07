package net.mx.eaddons.elplus;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 行刑者之斧的斩首：近战（直接攻击者是手持斧的玩家）击杀指定生物时，按概率额外掉落其头颅。
 * <p>与 EL+ 一致按类精确匹配，尸壳、流浪者等子类不算；已经掉了同种头颅时不重复。
 */
public class ExecutionAxeHandler {

    /** 生物类 → 原版头颅 meta（0 骷髅、1 凋灵骷髅、2 僵尸、4 苦力怕、5 末影龙） */
    private static final Map<Class<?>, Integer> SKULLS = new IdentityHashMap<>();

    static {
        SKULLS.put(EntitySkeleton.class, 0);
        SKULLS.put(EntityWitherSkeleton.class, 1);
        SKULLS.put(EntityZombie.class, 2);
        SKULLS.put(EntityCreeper.class, 4);
        SKULLS.put(EntityDragon.class, 5);
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote || !(event.getSource().getImmediateSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getImmediateSource();
        if (player.getHeldItemMainhand().getItem() != ItemExecutionAxe.INSTANCE) {
            return;
        }
        Integer meta = SKULLS.get(victim.getClass());
        if (meta == null) {
            return;
        }
        float chance = ElPlusConfig.beheadChance + ElPlusConfig.beheadLootingBonus * event.getLootingLevel();
        if (victim.world.rand.nextFloat() >= chance || hasSkull(event, meta)) {
            return;
        }
        event.getDrops().add(new EntityItem(victim.world, victim.posX, victim.posY, victim.posZ,
                new ItemStack(Items.SKULL, 1, meta)));
    }

    private static boolean hasSkull(LivingDropsEvent event, int meta) {
        for (EntityItem drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getItem() == Items.SKULL && stack.getMetadata() == meta) {
                return true;
            }
        }
        return false;
    }
}
