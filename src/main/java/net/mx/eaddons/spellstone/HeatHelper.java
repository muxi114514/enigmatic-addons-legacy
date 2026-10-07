package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 烈焰形态满级的「炽热」计数。
 *
 * <p>EL+ 原版直接读 {@code getRemainingFireTicks()}（即原版燃烧计时）来算增伤，但 1.12.2 这边
 * 有三处会把它清零：淋雨或入水（{@code Entity#onEntityUpdate} 开头的 {@code if (isWet()) extinguish()}）、
 * 饰品栏另戴一颗烈焰之核（{@code ItemMagmaHeart#onWornTick} 无条件 {@code extinguish()}）、
 * 以及实体级火免疫（{@code fire -= 4} 快速归零）。一下雨满级就退化成普通剑，故改为自建计数。
 *
 * <p>数值语义与原版燃烧一致（tick、每 tick 衰减 1、命中时按同一公式续），并在实体真的着火时
 * 与原版 fire 取齐，保住「主动点燃自己叠伤害」的玩法。
 *
 * <p>数据存实体 persistent NBT，随实体回收；只在服务端主线程读写。
 */
public final class HeatHelper {

    private static final String HEAT_TICKS = "EAddonsBlazeHeat";

    private HeatHelper() {
    }

    /**
     * 命中后续炽热，公式照搬 EL+ 的续燃烧：新值 = (旧值×0.05 + 5 + 等级) 秒，且只增不减。
     * 展开后等价于每次命中 +(5+等级) 秒，连续作战才堆得高。
     */
    public static void onHit(EntityLivingBase attacker, int spellLevel) {
        int heat = getHeat(attacker);
        int next = ((int) (heat * 0.05F) + 5 + spellLevel) * 20;
        if (next > heat) {
            attacker.getEntityData().setInteger(HEAT_TICKS, next);
        }
    }

    public static int getHeat(EntityLivingBase entity) {
        return entity.getEntityData().getInteger(HEAT_TICKS);
    }

    /** 炽热带来的伤害倍率增量，与 EL+ 同公式：(炽热/100)^0.9 × 5%。 */
    public static float getDamageBonus(EntityLivingBase attacker) {
        int heat = getHeat(attacker);
        if (heat <= 0) {
            return 0.0F;
        }
        return (float) Math.pow(heat / 100.0D, 0.9D) * 0.05F;
    }

    /** 每 tick 推进：衰减 1，并在实体真的着火时与原版燃烧取齐。 */
    public static void tick(EntityLivingBase entity) {
        NBTTagCompound tag = entity.getEntityData();
        // 没有炽热的实体在这里直接返回
        if (!tag.hasKey(HEAT_TICKS)) {
            // 站进火里自燃：未持有炽热时也能起头
            if (entity.isBurning() && SpellstoneData.isHolding(entity, SpellstoneForm.BLAZING_CORE)) {
                tag.setInteger(HEAT_TICKS, 20);
            }
            return;
        }
        int heat = tag.getInteger(HEAT_TICKS);
        if (heat <= 0) {
            tag.removeTag(HEAT_TICKS);
            return;
        }
        // 1.12.2 的 Entity.fire 是 private 且没有公开 getter，故改用「燃烧中不衰减」
        // 等效跟随原版燃烧：站在火里或岩浆里依旧能把炽热维持住
        tag.setInteger(HEAT_TICKS, entity.isBurning() ? heat : heat - 1);
    }

    public static void clear(EntityLivingBase entity) {
        entity.getEntityData().removeTag(HEAT_TICKS);
    }
}
