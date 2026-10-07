package net.mx.eaddons.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.mx.eaddons.spellstone.SpellstoneForm;

/**
 * 虚空调谐：受到致命伤害时按概率免死，剩 1 点生命并短暂完全无敌。
 *
 * <p>挂在原版的图腾保命检查上（见 {@code MixinEntityLivingTotem}）而不是伤害事件：装了 First Aid 后
 * 「致命」由它按身体部位判定，伤害数值未必 ≥ 当前生命；但它判定玩家该死时同样会调这个检查
 * （整合包配置 allowOtherHealingItems=true），通过后把头与躯干拉回至少 1 点。
 * 一处同时覆盖原版与 First Aid 两条死亡路径。
 */
public final class SpelltunerDeathWard {
    private static final String WARD_UNTIL = "EAddonsVoidWardUntil";

    private SpelltunerDeathWard() {
    }

    /** @return true 表示已免死，调用方应让图腾检查直接返回 true。 */
    public static boolean tryWard(EntityLivingBase entity, DamageSource source) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote || source.canHarmInCreative()) {
            return false;   // 与图腾一致：虚空与 /kill 不救
        }
        if (!ItemSpelltuner.hasTune(entity, SpellstoneForm.VOID_PEARL)
                || entity.getRNG().nextInt(100) >= SpelltunerConfig.voidWardChance) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity;
        player.setHealth(1.0F);
        player.getEntityData().setLong(WARD_UNTIL,
                player.world.getTotalWorldTime() + SpelltunerConfig.voidWardInvulnTicks);

        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(EnumParticleTypes.PORTAL, player.posX,
                    player.posY + player.height * 0.5D, player.posZ, 60, 0.4D, 0.8D, 0.4D, 0.4D);
        }
        return true;
    }

    /** 是否处在免死后的无敌窗口内。过期的记录顺手删掉，不留在存档里。 */
    public static boolean isWarded(EntityLivingBase entity, DamageSource source) {
        if (source.canHarmInCreative()) {
            return false;
        }
        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(WARD_UNTIL)) {
            return false;
        }
        if (entity.world.getTotalWorldTime() < data.getLong(WARD_UNTIL)) {
            return true;
        }
        data.removeTag(WARD_UNTIL);
        return false;
    }
}
