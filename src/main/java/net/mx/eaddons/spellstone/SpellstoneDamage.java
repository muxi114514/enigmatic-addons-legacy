package net.mx.eaddons.spellstone;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

/**
 * 共鸣者各形态用到的伤害源。
 *
 * <p>1.20.1 走数据驱动的 {@code DamageTypes} + Holder，1.12.2 直接 new 即可，反而简单。
 * 一律用 {@link EntityDamageSource} 带上攻击者——裸 {@link DamageSource} 不算玩家击杀，
 * 怪物不仇恨、不掉经验、不走抢夺。
 *
 * <p>damageType 沿用原版字符串，死亡消息直接复用原版翻译。
 */
public final class SpellstoneDamage {

    private SpellstoneDamage() {
    }

    /** 岩浆伤害（烈焰形态的喷火、岩浆爆）。 */
    public static DamageSource lava(Entity attacker) {
        return new EntityDamageSource("lava", attacker).setFireDamage();
    }

    /** 魔法伤害（星云瞬移斩、天使光束）。 */
    public static DamageSource magic(Entity attacker) {
        return (DamageSource) new EntityDamageSource("magic", attacker).setMagicDamage().setDamageBypassesArmor();
    }

    /** 落雷伤害（海洋形态）。 */
    public static DamageSource lightning(Entity attacker) {
        return new EntityDamageSource("lightningBolt", attacker);
    }

    /** 冰霜伤害（忘却形态的连锁），与忘却冰晶术石共用 damageType。 */
    public static DamageSource frost(Entity attacker) {
        return (DamageSource) new EntityDamageSource("eaddons.frost", attacker).setDamageBypassesArmor().setMagicDamage();
    }
}
