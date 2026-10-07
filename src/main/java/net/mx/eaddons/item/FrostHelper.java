package net.mx.eaddons.item;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.mx.eaddons.compat.IceAndFireCompat;
import net.mx.eaddons.compat.ModCompat;

import java.util.UUID;

/**
 * 忘却冰晶的自建冻结。
 *
 * <p>为什么不直接用冰与火的冻结：它的 severity 0 会被燃烧解除（{@code EntityEffectHandler} 里
 * 目标燃烧就 extinguish + reset），而 severity 1 又是彻底定身，近战每刀定身过强。
 * 本模组要的是「只减速、且火烧不掉」，两档都不符合，故逻辑自建、只借它的冰壳渲染。
 *
 * <p>数据存在实体的 persistent NBT（{@link net.minecraft.entity.Entity#getEntityData()}）上，
 * 随实体生命周期回收，无需注册 capability、无需额外集合，不会内存泄漏。
 * 减速用属性修饰符而非直接改 motion：motion 对客户端权威移动的玩家无效，属性会同步到客户端。
 *
 * <p>线程安全：所有方法只在服务端主线程（tick / 事件）调用。
 */
public final class FrostHelper {

    /** 剩余冻结 tick。 */
    private static final String FROST_TICKS = "EAddonsFrostTicks";

    private static final UUID SLOW_UUID = UUID.fromString("8c2f9e41-6d3b-4a75-9f10-5e7c3a1d0b62");
    private static final String SLOW_NAME = "ForgottenIceFrost";

    private FrostHelper() {
    }

    /** 施加冻结，与现有时长取较大值（连续攻击不会无限叠加）。 */
    public static void applyFrost(EntityLivingBase target, int ticks) {
        if (target == null || ticks <= 0 || target.world.isRemote) {
            return;
        }
        NBTTagCompound tag = target.getEntityData();
        if (ticks > tag.getInteger(FROST_TICKS)) {
            tag.setInteger(FROST_TICKS, ticks);
        }
    }

    /** 剩余冻结 tick，供共鸣者忘却形态按冻结程度算增伤。 */
    public static int getFrostTicks(EntityLivingBase entity) {
        return entity == null ? 0 : entity.getEntityData().getInteger(FROST_TICKS);
    }

    /** 本模组的冻结是否生效。 */
    public static boolean hasFrost(EntityLivingBase entity) {
        return entity != null && entity.getEntityData().getInteger(FROST_TICKS) > 0;
    }

    /** 统一的「处于冻结」判定：本模组冻结，或冰与火的冻结（冰龙冰息等）。 */
    public static boolean isFrozen(EntityLivingBase entity) {
        if (hasFrost(entity)) {
            return true;
        }
        return ModCompat.ICE_AND_FIRE && IceAndFireCompat.isFrozenByIaf(entity);
    }

    /** 立即解除本模组的冻结并移除减速。 */
    public static void clearFrost(EntityLivingBase entity) {
        entity.getEntityData().removeTag(FROST_TICKS);
        removeSlowdown(entity);
    }

    /**
     * 每 tick 推进：倒计时、维持减速、续冰壳。
     * 属性修饰符不随实体存盘，而冻结 tick 存在 persistent NBT 里，重进世界后由本方法自动补回。
     */
    public static void tick(EntityLivingBase entity) {
        NBTTagCompound tag = entity.getEntityData();
        // 没被冻的实体在这里直接返回，每 tick 只多一次键存在性检查
        if (!tag.hasKey(FROST_TICKS)) {
            return;
        }
        int left = tag.getInteger(FROST_TICKS);
        if (left <= 0) {
            tag.removeTag(FROST_TICKS);
            removeSlowdown(entity);
            return;
        }

        tag.setInteger(FROST_TICKS, left - 1);
        applySlowdown(entity);
        if (ModCompat.ICE_AND_FIRE) {
            IceAndFireCompat.renderFrost(entity);
        }
    }

    // ============================ 减速 ============================

    private static void applySlowdown(EntityLivingBase entity) {
        IAttributeInstance inst = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (inst == null || inst.getModifier(SLOW_UUID) != null) {
            return;
        }
        // operation 1：按基础值的百分比扣减，与原版缓慢药水同档
        double amount = -Math.min(ForgottenIceConfig.slowdownPercent, 99) / 100.0D;
        inst.applyModifier(new AttributeModifier(SLOW_UUID, SLOW_NAME, amount, 1));
    }

    private static void removeSlowdown(EntityLivingBase entity) {
        IAttributeInstance inst = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (inst != null && inst.getModifier(SLOW_UUID) != null) {
            inst.removeModifier(SLOW_UUID);
        }
    }

    // ============================ BOSS 判定 ============================

    /**
     * BOSS 判定，三条任一命中即算：原版 {@code isNonBoss}、配置的注册名列表、最大生命阈值。
     * <p>只靠 {@code isNonBoss} 不够——1.12.2 里只有末影龙与凋零重写了它，
     * 恐怖生物、冰与火的龙、巧克力任务的 BOSS 都会被当成普通怪。
     */
    public static boolean isBoss(EntityLivingBase entity) {
        if (!entity.isNonBoss()) {
            return true;
        }
        if (ForgottenIceConfig.bossHealthThreshold > 0
                && entity.getMaxHealth() >= ForgottenIceConfig.bossHealthThreshold) {
            return true;
        }
        if (ForgottenIceConfig.bossEntities.length > 0) {
            ResourceLocation key = EntityList.getKey(entity);
            if (key != null) {
                String id = key.toString();
                for (String listed : ForgottenIceConfig.bossEntities) {
                    if (id.equalsIgnoreCase(listed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 按 BOSS 与否换算实际冻结时长。 */
    public static int frostTimeFor(EntityLivingBase target, int baseTicks) {
        return isBoss(target) ? baseTicks / Math.max(1, ForgottenIceConfig.bossFrostDivisor) : baseTicks;
    }
}
