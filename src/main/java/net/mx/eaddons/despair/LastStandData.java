package net.mx.eaddons.despair;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

/**
 * 绝境的玩家数据，存在 PlayerPersisted 下：退出重进、死亡重生、从末地回来时 Forge 都会一并保留。
 * <ul>
 * <li>Timer：绝境剩余 tick，大于 0 表示正在绝境中</li>
 * <li>ReadyAt：冷却结束时的世界总时间</li>
 * <li>Doom：失败处决的重试次数，大于 0 表示正在处决</li>
 * </ul>
 */
final class LastStandData {
    static final String TIMER = "Timer";
    static final String READY_AT = "ReadyAt";
    static final String DOOM = "Doom";
    private static final String KEY = "EAddonsLastStand";

    private LastStandData() {
    }

    /** 只读查看，没有数据时返回 null 且不创建空标签（每 tick 都会调用）。 */
    @Nullable
    static NBTTagCompound peek(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        return persisted.hasKey(KEY, Constants.NBT.TAG_COMPOUND) ? persisted.getCompoundTag(KEY) : null;
    }

    static NBTTagCompound getOrCreate(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        NBTTagCompound tag = persisted.getCompoundTag(KEY);
        persisted.setTag(KEY, tag);
        return tag;
    }

    /** 各项都清掉后删除整个标签，不在存档里留空壳。 */
    static void removeIfEmpty(EntityPlayer player) {
        NBTTagCompound tag = peek(player);
        if (tag != null && tag.hasNoTags()) {
            player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).removeTag(KEY);
        }
    }

    static int timer(EntityPlayer player) {
        NBTTagCompound tag = peek(player);
        return tag == null ? 0 : tag.getInteger(TIMER);
    }

    static long readyAt(EntityPlayer player) {
        NBTTagCompound tag = peek(player);
        return tag == null ? 0L : tag.getLong(READY_AT);
    }
}
