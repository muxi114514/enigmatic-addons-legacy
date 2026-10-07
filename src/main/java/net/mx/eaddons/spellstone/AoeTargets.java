package net.mx.eaddons.spellstone;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.mx.eaddons.item.SpellstoneFormConfig;

/**
 * 共鸣者范围技能的误伤过滤：不打自己、队友与自己驯服的生物；其他玩家只在服务器开着 PvP 时才打。
 * 单体技能（光束、魂焰弹、落雷）是瞄准出手，不经过这里。
 */
public final class AoeTargets {

    private AoeTargets() {
    }

    public static boolean canHit(EntityLivingBase attacker, Entity target) {
        if (target == attacker || !(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            return false;
        }
        if (!SpellstoneFormConfig.aoeSparesAllies) {
            return true;
        }
        if (attacker.isOnSameTeam(target)) {
            return false;
        }
        if (target instanceof IEntityOwnable
                && attacker.getUniqueID().equals(((IEntityOwnable) target).getOwnerId())) {
            return false;
        }
        if (target instanceof EntityPlayer) {
            MinecraftServer server = target.world.getMinecraftServer();
            return server != null && server.isPVPEnabled() && (!(attacker instanceof EntityPlayer)
                    || ((EntityPlayer) attacker).canAttackPlayer((EntityPlayer) target));
        }
        return true;
    }
}
