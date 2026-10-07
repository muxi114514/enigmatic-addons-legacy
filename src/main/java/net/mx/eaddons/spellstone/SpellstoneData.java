package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * 术质共鸣者剑上的持久化数据（NBT 层）。
 *
 * <p>1.21 的 EL+ 用 DataComponents 存这些字段，1.20.1 的移植版已经退回纯 NBT，
 * 因此本类几乎是逐字平移：{@code CompoundTag → NBTTagCompound}、
 * {@code ItemStack.of → new ItemStack(nbt)}、{@code save → writeToNBT}。
 *
 * <p>所有读写集中在这里，避免逻辑里散落裸 NBT 键。
 */
public final class SpellstoneData {

    public static final int MAX_LEVEL = 5;

    private static final String RESONANCE = "Resonance";
    private static final String LEVEL = "SpellLevel";
    private static final String ENERGY = "Energy";
    private static final String ANCHOR_DIM = "AnchorDim";
    private static final String ANCHOR_POS = "AnchorPos";
    private static final String CONFIRM = "ResonanceConfirm";
    /** 链接的立方形态序号（11 原初 / 12 非欧）。链接不存物品，只记形态，解除时也就无从复制出术石。 */
    private static final String LINK = "ResonanceLink";
    /** 原初链接下当前选中的子形态序号，0 表示纯原初。 */
    private static final String SUB = "ResonanceSub";
    /** 是否佩戴着对应术石。写在 NBT 上而非每次现算，属性修饰符才会在戴上/摘下的那一刻刷新。 */
    private static final String LINK_ACTIVE = "ResonanceLinkOn";
    /** 自建计时的冷却表（形态主动里冷却超过原版遮罩可用范围的走这里）。 */
    private static final String TIMERS = "ResonanceTimers";

    private SpellstoneData() {
    }

    private static NBTTagCompound tag(ItemStack sword) {
        NBTTagCompound tag = sword.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            sword.setTagCompound(tag);
        }
        return tag;
    }

    // ============================ 共鸣术石 ============================

    public static ItemStack getResonance(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(RESONANCE, 10)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(nbt.getCompoundTag(RESONANCE));
    }

    public static void setResonance(ItemStack sword, ItemStack spellstone) {
        tag(sword).setTag(RESONANCE, spellstone.writeToNBT(new NBTTagCompound()));
    }

    public static boolean hasResonance(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt != null && nbt.hasKey(RESONANCE, 10);
    }

    public static void clearResonance(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt != null) {
            nbt.removeTag(RESONANCE);
        }
    }

    /**
     * 当前生效的形态。吃入式共鸣直接看术石；链接式（两颗立方）要佩戴着对应术石才生效，
     * 原初链接下返回的是当前选中的子形态，所以十个形态的既有分发逻辑原样复用。
     */
    public static SpellstoneForm getForm(ItemStack sword) {
        if (hasResonance(sword)) {
            return SpellstoneForm.byStack(getResonance(sword));
        }
        SpellstoneForm link = getLinkForm(sword);
        if (link == SpellstoneForm.NONE || !isLinkActive(sword)) {
            return SpellstoneForm.NONE;
        }
        return link == SpellstoneForm.PRIMEVAL_CUBE ? getSubForm(sword) : link;
    }

    public static boolean isResonatingWith(ItemStack sword, SpellstoneForm form) {
        return getForm(sword) == form;
    }

    // ============================ 链接式共鸣（原初 / 非欧立方） ============================

    /** 链接的立方形态；没链接返回 NONE。不判断是否佩戴。 */
    public static SpellstoneForm getLinkForm(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(LINK)) {
            return SpellstoneForm.NONE;
        }
        SpellstoneForm form = SpellstoneForm.byIndex(nbt.getInteger(LINK));
        return form.isLink() ? form : SpellstoneForm.NONE;
    }

    public static boolean hasLink(ItemStack sword) {
        return getLinkForm(sword) != SpellstoneForm.NONE;
    }

    public static void setLink(ItemStack sword, SpellstoneForm form) {
        NBTTagCompound nbt = tag(sword);
        nbt.setInteger(LINK, form.index());
        nbt.setInteger(SUB, 0);
        nbt.setBoolean(LINK_ACTIVE, false);
    }

    public static void clearLink(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt != null) {
            nbt.removeTag(LINK);
            nbt.removeTag(SUB);
            nbt.removeTag(LINK_ACTIVE);
        }
    }

    /** 是否佩戴着链接的那颗术石（没佩戴时整把剑按未共鸣处理，即休眠）。 */
    public static boolean isLinkActive(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt != null && nbt.getBoolean(LINK_ACTIVE);
    }

    public static void setLinkActive(ItemStack sword, boolean active) {
        tag(sword).setBoolean(LINK_ACTIVE, active);
    }

    /** 原初链接下选中的子形态，纯原初返回 {@link SpellstoneForm#PRIMEVAL_CUBE}。 */
    public static SpellstoneForm getSubForm(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        int index = nbt == null ? 0 : nbt.getInteger(SUB);
        return index == 0 ? SpellstoneForm.PRIMEVAL_CUBE : SpellstoneForm.byIndex(index);
    }

    public static void setSubForm(ItemStack sword, SpellstoneForm form) {
        tag(sword).setInteger(SUB, form == SpellstoneForm.PRIMEVAL_CUBE ? 0 : form.index());
    }

    /** 原初共鸣生效中（四条被动的统一判定）。 */
    public static boolean isPrimevalActive(ItemStack sword) {
        return getLinkForm(sword) == SpellstoneForm.PRIMEVAL_CUBE && isLinkActive(sword);
    }

    /** 非欧共鸣生效中。 */
    public static boolean isCubeActive(ItemStack sword) {
        return getLinkForm(sword) == SpellstoneForm.THE_CUBE && isLinkActive(sword);
    }

    /** 物品栏里显示用的形态：链接期间始终显示立方本身（贴图与名字不随子形态变）。 */
    public static SpellstoneForm getDisplayForm(ItemStack sword) {
        SpellstoneForm link = getLinkForm(sword);
        return link != SpellstoneForm.NONE && isLinkActive(sword) ? link : getForm(sword);
    }

    // ============================ 共鸣等级 ============================

    public static int getLevel(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt == null ? 0 : nbt.getInteger(LEVEL);
    }

    public static void setLevel(ItemStack sword, int level) {
        tag(sword).setInteger(LEVEL, Math.max(0, Math.min(MAX_LEVEL, level)));
    }

    /** 是否满级（EL+ 的各形态额外效果都判 level > 4）。 */
    public static boolean isMaxLevel(ItemStack sword) {
        return getLevel(sword) >= MAX_LEVEL;
    }

    // ============================ 能量 ============================

    public static int getEnergy(ItemStack sword) {
        if (isEnergyAlwaysFull(sword)) {
            return getMaxEnergy(sword);
        }
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt == null ? 0 : nbt.getInteger(ENERGY);
    }

    public static void setEnergy(ItemStack sword, int energy) {
        if (isEnergyAlwaysFull(sword)) {
            return;   // 恒满，消耗与积攒都无意义，也省掉每次开火的 NBT 同步
        }
        tag(sword).setInteger(ENERGY, Math.max(0, Math.min(getMaxEnergy(sword), energy)));
    }

    /**
     * 原初共鸣下子形态能量恒满。忘却（能量即冰盾）、复苏（满能量回血）、引擎（满能量 ×2.5）
     * 三个形态的能量是「攒满即触发」而非消耗品，恒满会直接失控，所以排除在外照常积攒。
     */
    public static boolean isEnergyAlwaysFull(ItemStack sword) {
        if (!isPrimevalActive(sword)) {
            return false;
        }
        switch (getSubForm(sword)) {
            case FORGOTTEN_ICE:
            case REVIVAL_LEAF:
            case LOST_ENGINE:
                return false;
            default:
                return true;
        }
    }

    public static void addEnergy(ItemStack sword, int delta) {
        setEnergy(sword, getEnergy(sword) + delta);
    }

    /** 无视「恒满」直接把存的能量清零，换形态时用，免得上一形态的数值被下一形态读到。 */
    public static void resetEnergy(ItemStack sword) {
        tag(sword).setInteger(ENERGY, 0);
    }

    public static int getMaxEnergy(ItemStack sword) {
        return getForm(sword).maxEnergy(getLevel(sword));
    }

    // ============================ 星云锚点 ============================

    public static boolean hasAnchor(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt != null && nbt.hasKey(ANCHOR_POS);
    }

    public static int getAnchorDim(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt == null ? 0 : nbt.getInteger(ANCHOR_DIM);
    }

    @Nullable
    public static BlockPos getAnchorPos(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(ANCHOR_POS)) {
            return null;
        }
        return BlockPos.fromLong(nbt.getLong(ANCHOR_POS));
    }

    public static void setAnchor(ItemStack sword, int dimension, BlockPos pos) {
        NBTTagCompound nbt = tag(sword);
        nbt.setInteger(ANCHOR_DIM, dimension);
        nbt.setLong(ANCHOR_POS, pos.toLong());
    }

    public static void clearAnchor(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt != null) {
            nbt.removeTag(ANCHOR_DIM);
            nbt.removeTag(ANCHOR_POS);
        }
    }

    // ============================ 共鸣切换的二次确认窗口 ============================

    /**
     * 共鸣的建立与解除不走原版读条（{@code setActiveHand} + {@code onItemUseFinish}），
     * 改成「窗口内连按两次右键」。
     *
     * <p>原因：右键那一下要改剑的 NBT，服务端随即用 {@code SPacketSetSlot} 把整个
     * ItemStack 同步给客户端，客户端的实例被替换；而 {@code EntityLivingBase#updateActiveHand}
     * 是用 {@code itemstack == this.activeItemStack} 的引用比较来判断读条是否有效的，
     * 引用一失配就 {@code resetActiveHand()}。两端因此各自重开读条、计时错位，
     * 在部分形态下表现为读条走完也不生效。这套确认窗口只依赖 onItemRightClick 与
     * onUpdate，完全绕开上面这条链路。
     */
    public static int getConfirm(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        return nbt == null ? 0 : nbt.getInteger(CONFIRM);
    }

    public static void setConfirm(ItemStack sword, int ticks) {
        if (ticks <= 0) {
            NBTTagCompound nbt = sword.getTagCompound();
            if (nbt != null) {
                nbt.removeTag(CONFIRM);
            }
            return;
        }
        tag(sword).setInteger(CONFIRM, ticks);
    }

    /** 每 tick 递减，由 inventoryTick 调用。 */
    public static void tickConfirm(ItemStack sword) {
        countDown(sword, CONFIRM);
    }

    // ============================ 自建计时的技能冷却 ============================

    /**
     * 原版 {@code CooldownTracker} 按物品记：一旦挂上长冷却，这把剑的整个右键（含解除共鸣、
     * 其它形态的主动）都会被挡住。所以只有短冷却走原版，长冷却存在剑上自己数。
     */
    public static int getTimer(ItemStack sword, String key) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(TIMERS, 10)) {
            return 0;
        }
        return nbt.getCompoundTag(TIMERS).getInteger(key);
    }

    public static void setTimer(ItemStack sword, String key, int ticks) {
        NBTTagCompound nbt = tag(sword);
        NBTTagCompound timers = nbt.hasKey(TIMERS, 10) ? nbt.getCompoundTag(TIMERS) : new NBTTagCompound();
        if (ticks <= 0) {
            timers.removeTag(key);
        } else {
            timers.setInteger(key, ticks);
        }
        nbt.setTag(TIMERS, timers);
    }

    /** 每 tick 递减全部自建计时；空了就把整个表删掉。 */
    public static void tickTimers(ItemStack sword) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(TIMERS, 10)) {
            return;
        }
        NBTTagCompound timers = nbt.getCompoundTag(TIMERS);
        for (String key : new java.util.ArrayList<>(timers.getKeySet())) {
            int left = timers.getInteger(key) - 1;
            if (left <= 0) {
                timers.removeTag(key);
            } else {
                timers.setInteger(key, left);
            }
        }
        if (timers.hasNoTags()) {
            nbt.removeTag(TIMERS);
        }
    }

    /** 把 NBT 上的一个计时键减 1，减到 0 就删掉，避免留下空键。 */
    private static void countDown(ItemStack sword, String key) {
        NBTTagCompound nbt = sword.getTagCompound();
        if (nbt == null || !nbt.hasKey(key)) {
            return;
        }
        int left = nbt.getInteger(key);
        if (left <= 1) {
            nbt.removeTag(key);
        } else {
            nbt.setInteger(key, left - 1);
        }
    }

    // ============================ 玩家身上的活跃抓钩 ============================

    private static final String ENGINE_HOOK = "EAddonsEngineHook";

    /** 记录/清除玩家当前的抓钩 UUID，替代 1.20.1 的数据附件。 */
    public static void setPlayerEngineHook(EntityLivingBase owner, @Nullable java.util.UUID uuid) {
        if (uuid == null) {
            owner.getEntityData().removeTag(ENGINE_HOOK);
        } else {
            owner.getEntityData().setUniqueId(ENGINE_HOOK, uuid);
        }
    }

    @Nullable
    public static java.util.UUID getPlayerEngineHook(EntityLivingBase owner) {
        NBTTagCompound tag = owner.getEntityData();
        return tag.hasUniqueId(ENGINE_HOOK) ? tag.getUniqueId(ENGINE_HOOK) : null;
    }

    // ============================ 便捷判定 ============================

    /** 该实体主手是否持有共鸣于指定形态的共鸣者。 */
    public static boolean isHolding(EntityLivingBase entity, SpellstoneForm form) {
        ItemStack held = entity.getHeldItemMainhand();
        return held.getItem() instanceof net.mx.eaddons.item.ItemSpellstoneSword
                && isResonatingWith(held, form);
    }

    public static boolean isCreative(EntityLivingBase user) {
        return user instanceof EntityPlayer && ((EntityPlayer) user).capabilities.isCreativeMode;
    }
}
