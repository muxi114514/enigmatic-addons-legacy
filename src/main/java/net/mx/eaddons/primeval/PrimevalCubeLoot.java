package net.mx.eaddons.primeval;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.Random;

/**
 * 原初立方的获取：照搬神秘遗物休眠之眼的做法——给所有原版箱子战利品表（{@code minecraft:chests/*}）注入一个条目，
 * 开箱玩家身上没有标记就放一个进去并打上标记。于是每人只在自己打开的第一个原版战利品箱里拿到一次；
 * 其他模组自己的战利品表不算。老存档里已开过箱子的玩家会在下一个打开的原版箱子里拿到。
 */
public class PrimevalCubeLoot {
    private static final String CHEST_PREFIX = "minecraft:chests/";
    private static final String LOOTED = "EAddonsLootedPrimevalCube";

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().toString().startsWith(CHEST_PREFIX)) {
            return;
        }
        event.getTable().addPool(new LootPool(new LootEntry[]{new FirstChestEntry()}, new LootCondition[0],
                new RandomValueRange(1.0F), new RandomValueRange(0.0F), "eaddons_primeval_cube_first"));
    }

    /** 标记存在玩家的持久数据里，死亡重生后仍在。 */
    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            root.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    /** 只在开箱者还没拿过时产出一个原初立方。 */
    private static class FirstChestEntry extends LootEntry {

        FirstChestEntry() {
            super(1, 0, new LootCondition[0], "eaddons_primeval_cube");
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
            // 箱子的战利品上下文里，开箱玩家就放在 killerPlayer 这个位置
            Entity looter = context.getKillerPlayer();
            if (!(looter instanceof EntityPlayer)) {
                return;
            }
            NBTTagCompound data = persisted((EntityPlayer) looter);
            if (data.getBoolean(LOOTED)) {
                return;
            }
            data.setBoolean(LOOTED, true);
            stacks.add(new ItemStack(ItemPrimevalCube.INSTANCE));
        }

        @Override
        protected void serialize(JsonObject json, JsonSerializationContext context) {
        }
    }
}
