package net.mx.eaddons.item;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.mx.eaddons.spellstone.SpellstoneForm;

/**
 * 冥灯调谐：普通亡灵（UNDEAD 属性、非 Boss、不在亡灵中立名单里）对佩戴者中立。
 * 不会主动把佩戴者设为目标；被佩戴者打了的那一只，可在
 * {@link SpelltunerConfig#lanternProvokeSeconds} 秒内反击他，每次挨打都刷新。
 * 挑衅记录存在这只亡灵自己的实体数据里，不会牵连同类。
 */
public final class SpelltunerUndeadTruce {
    private static final String PROVOKER = "EAddonsTruceProvoker";
    private static final String PROVOKED_UNTIL = "EAddonsTruceUntil";
    private static final double SCAN_RADIUS = 32.0D;

    private SpelltunerUndeadTruce() {
    }

    static boolean isTruceMob(EntityLivingBase entity) {
        return entity instanceof EntityLiving && entity.isNonBoss()
                && entity.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD
                && !IllusionLanternConfig.isTruceBlacklisted(entity);
    }

    /**
     * 设目标事件：原版先写字段再发事件，所以这里 {@code getAttackTarget()} 已是新目标，清空即可。
     * 受击反击（setRevengeTarget）也发同一个事件，但不改攻击目标，靠「攻击目标就是佩戴者」这一条排除掉。
     */
    static void onSetTarget(EntityLivingBase mob, EntityLivingBase target) {
        if (!(target instanceof EntityPlayer) || mob.world.isRemote || !isTruceMob(mob)) {
            return;
        }
        EntityLiving living = (EntityLiving) mob;
        EntityPlayer player = (EntityPlayer) target;
        if (living.getAttackTarget() == player && !isProvokedBy(mob, player)
                && ItemSpelltuner.hasTune(player, SpellstoneForm.ILLUSION_LANTERN)) {
            living.setAttackTarget(null);
        }
    }

    /** 佩戴者攻击了亡灵：记下挑衅者与期限。须在反击目标写入之前调用。 */
    static void markProvoked(EntityLivingBase mob, EntityPlayer player) {
        if (!isTruceMob(mob) || !ItemSpelltuner.hasTune(player, SpellstoneForm.ILLUSION_LANTERN)) {
            return;
        }
        NBTTagCompound data = mob.getEntityData();
        data.setString(PROVOKER, player.getUniqueID().toString());
        data.setLong(PROVOKED_UNTIL, mob.world.getTotalWorldTime() + SpelltunerConfig.lanternProvokeSeconds * 20L);
    }

    static boolean isProvokedBy(EntityLivingBase mob, EntityPlayer player) {
        NBTTagCompound data = mob.getEntityData();
        return data.hasKey(PROVOKED_UNTIL)
                && mob.world.getTotalWorldTime() < data.getLong(PROVOKED_UNTIL)
                && player.getUniqueID().toString().equals(data.getString(PROVOKER));
    }

    /**
     * 每秒一次：戴上调谐器之前就锁定了佩戴者、或挑衅期已过还在追的亡灵，放下目标。
     * {@code getEntitiesWithinAABB} 只遍历已加载的区块，不会触发加载。
     */
    static void scan(EntityPlayer player) {
        AxisAlignedBB box = player.getEntityBoundingBox().grow(SCAN_RADIUS);
        for (EntityLiving mob : player.world.getEntitiesWithinAABB(EntityLiving.class, box,
                m -> m != null && m.getAttackTarget() == player)) {
            if (isTruceMob(mob) && !isProvokedBy(mob, player)) {
                mob.setAttackTarget(null);
            }
        }
    }
}
