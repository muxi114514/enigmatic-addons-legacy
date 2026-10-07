package net.mx.eaddons.primeval;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;

/**
 * 原初立方（进度物品）死亡不掉落。
 *
 * <p>取 HIGHEST：整合包的墓碑模组在 HIGH 处理自己的灵魂绑定、在 LOW 建墓，要赶在它们之前从掉落列表里拿走。
 * 拿走的物品先暂存在死前玩家的实体数据里，克隆时转给新玩家，重生落地后再放回背包——
 * 克隆那一刻新玩家还没进世界，这时往外掉东西不安全。
 */
public class PrimevalCubeSoulbound {
    private static final String KEPT = "EAddonsSoulboundItems";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            return;
        }
        NBTTagList kept = null;
        Iterator<EntityItem> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next().getItem();
            if (stack.getItem() instanceof ItemPrimevalCube) {
                if (kept == null) {
                    kept = new NBTTagList();
                }
                kept.appendTag(stack.writeToNBT(new NBTTagCompound()));
                it.remove();
            }
        }
        if (kept != null) {
            player.getEntityData().setTag(KEPT, kept);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        NBTTagCompound old = event.getOriginal().getEntityData();
        if (old.hasKey(KEPT)) {
            event.getEntityPlayer().getEntityData().setTag(KEPT, old.getTag(KEPT).copy());
            old.removeTag(KEPT);
        }
    }

    @SubscribeEvent
    public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        if (player.world.isRemote || !data.hasKey(KEPT)) {
            return;
        }
        NBTTagList list = data.getTagList(KEPT, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            if (!stack.isEmpty() && !player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }
        }
        data.removeTag(KEPT);
    }
}
