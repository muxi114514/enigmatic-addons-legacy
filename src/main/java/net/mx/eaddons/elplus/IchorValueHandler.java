package net.mx.eaddons.elplus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.mx.eaddons.potion.PotionPureResistance;

/**
 * 灵液值：吃灵液根质累积，每 tick 衰减 1；超过阈值时给 15 秒纯化抗性并降到三分之一。
 * <p>与 1.20 版一致存在实体数据里，死亡后清零。
 */
public class IchorValueHandler {

    private static final String KEY = "EAddonsIchor";
    private static final int THRESHOLD = 1920;
    private static final int RESISTANCE_TICKS = 300;

    public static void add(EntityPlayer player, int amount) {
        NBTTagCompound data = player.getEntityData();
        data.setInteger(KEY, data.getInteger(KEY) + amount);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        NBTTagCompound data = event.player.getEntityData();
        if (!data.hasKey(KEY)) {
            return;
        }
        int ichor = data.getInteger(KEY);
        if (ichor <= 0) {
            data.removeTag(KEY);
        } else if (ichor > THRESHOLD) {
            EntityPlayer player = event.player;
            player.addPotionEffect(new PotionEffect(PotionPureResistance.INSTANCE, RESISTANCE_TICKS, 0));
            player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                    SoundCategory.AMBIENT, 1.0F, 1.0F);
            data.setInteger(KEY, ichor / 3);
        } else {
            data.setInteger(KEY, ichor - 1);
        }
    }
}
