package net.mx.eaddons.primeval;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.util.PotionRefresh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 原初立方（术石）的效果记录：存在术石物品的 NBT 上，换人、换存档都跟着物品走。
 * <p>记下的是「效果 + 记录那一刻的等级」，佩戴期间持续给予「记录等级 +1」（不超过配置上限）。
 */
public final class PrimevalRecord {
    private static final String TAG = "PrimevalRecord";

    /** 一条记录：效果与记录时的等级（amplifier）。 */
    public static final class Entry {
        public final Potion potion;
        public final int base;

        public Entry(Potion potion, int base) {
            this.potion = potion;
            this.base = base;
        }
    }

    private PrimevalRecord() {
    }

    // ============================ 读写 ============================

    public static List<Entry> read(ItemStack stone) {
        NBTTagCompound nbt = stone.getTagCompound();
        if (nbt == null || !nbt.hasKey(TAG)) {
            return Collections.emptyList();
        }
        List<Entry> entries = new ArrayList<>();
        NBTTagList list = nbt.getTagList(TAG, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(tag.getString("Id")));
            if (potion != null) {
                entries.add(new Entry(potion, tag.getInteger("Amp")));
            }
        }
        return entries;
    }

    public static void write(ItemStack stone, List<Entry> entries) {
        NBTTagCompound nbt = stone.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stone.setTagCompound(nbt);
        }
        NBTTagList list = new NBTTagList();
        for (Entry entry : entries) {
            ResourceLocation id = entry.potion.getRegistryName();
            if (id == null) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", id.toString());
            tag.setInteger("Amp", entry.base);
            list.appendTag(tag);
        }
        nbt.setTag(TAG, list);
    }

    // ============================ 等级 ============================

    /** 给予等级：记录等级 +1，不超过上限；记录等级本身已到上限的维持原样。 */
    public static int givenAmplifier(int base) {
        return base >= PrimevalConfig.buffLevelCap ? base : base + 1;
    }

    /**
     * 身上某个效果若是本术石正在给予的（在记录里、且等级正是给予等级），重新记录时按原记录等级算，
     * 不然每重录一次就再 +1。其余效果按当前等级。
     */
    public static int recordableAmplifier(List<Entry> record, PotionEffect effect) {
        for (Entry entry : record) {
            if (entry.potion == effect.getPotion() && effect.getAmplifier() == givenAmplifier(entry.base)) {
                return entry.base;
            }
        }
        return effect.getAmplifier();
    }

    // ============================ 佩戴期间持续给予 ============================

    /** 每 tick 调用；剩余不足 1 秒时续回满额，于是图标上的倒计时一直停在 4~5 秒。 */
    public static void supply(EntityPlayer player, ItemStack stone) {
        int duration = PrimevalConfig.recordEffectDuration;
        for (Entry entry : read(stone)) {
            PotionRefresh.ensure(player, entry.potion, givenAmplifier(entry.base), duration, duration - 20,
                    true, false);
        }
    }

    // ============================ 处理客户端发来的选择 ============================

    /**
     * 服务端核对后写入：必须正戴着原初立方术石、不在冷却中；只收身上确实有的非负面效果，
     * 等级由服务端按实际效果计算，不信客户端。空选择什么都不做，清空不进冷却。
     */
    public static void applySelection(EntityPlayerMP player, List<String> ids, boolean clear) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        int slot = BaublesApi.isBaubleEquipped(player, ItemPrimevalSpellstone.INSTANCE);
        if (handler == null || slot == -1) {
            return;
        }
        ItemStack stone = handler.getStackInSlot(slot);

        if (clear) {
            write(stone, Collections.emptyList());
            handler.setChanged(slot, true);
            player.sendStatusMessage(new TextComponentTranslation("message.eaddons.primeval_record.cleared"), true);
            return;
        }
        if (ids.isEmpty() || IForbiddenConsumed.get(player).getSpellstoneCooldown() > 0) {
            return;
        }

        List<Entry> current = read(stone);
        List<Entry> chosen = new ArrayList<>();
        Set<Potion> seen = new HashSet<>();
        for (String id : ids) {
            if (chosen.size() >= PrimevalConfig.recordSlots) {
                break;
            }
            Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(id));
            if (potion == null || potion.isBadEffect() || !seen.add(potion)) {
                continue;
            }
            PotionEffect effect = player.getActivePotionEffect(potion);
            if (effect != null) {
                chosen.add(new Entry(potion, recordableAmplifier(current, effect)));
            }
        }
        if (chosen.isEmpty()) {
            return;
        }

        write(stone, chosen);
        handler.setChanged(slot, true);
        IForbiddenConsumed.get(player).setSpellstoneCooldown(PrimevalConfig.recordCooldown);
        player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                SoundCategory.PLAYERS, 1.0F, 1.4F);
        player.sendStatusMessage(new TextComponentTranslation("message.eaddons.primeval_record.saved",
                chosen.size()), true);
    }
}
