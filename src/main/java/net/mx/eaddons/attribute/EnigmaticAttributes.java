package net.mx.eaddons.attribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 神秘遗物系的自定义属性（对应 1.21 EL+ 的 EnigmaticAttributes，并补上 1.12 原版缺的几项）。
 * <p>EL 1.12 原本把这些效果写死在事件里，现统一由属性承载：物品/状态只负责挂修饰符，
 * 结算集中在本包的各处理器里，附属模组（如神秘佳肴）直接给属性挂修饰符即可叠加。
 * <p>注册在 {@link EntityEvent.EntityConstructing}：该事件早于实体读档，
 * 放到 EntityJoinWorld 再注册会让存档里的修饰符被当作「未知属性」丢弃。
 */
public final class EnigmaticAttributes {

    /** 吸血：造成伤害后回复「最终伤害 × 值」的生命（含弹射物） */
    public static final IAttribute LIFESTEAL = create("eaddons.lifesteal", 0, 0, 64);
    /** 投射物偏转：被射中时按此概率反弹 */
    public static final IAttribute PROJECTILE_DEFLECT = create("eaddons.projectileDeflect", 0, 0, 1);
    /** 以太护盾：生命比例不高于此值时护盾生效（≥1 即常驻） */
    public static final IAttribute ETHERIUM_SHIELD = create("eaddons.etheriumShield", 0, 0, 1);
    /** 挖掘加速：新速度 += 原速度 × 值 */
    public static final IAttribute MINING_SPEED = create("eaddons.miningSpeed", 0, 0, 1024);
    /** 下落速度倍率：下落时每 tick 竖直速度 × 值（小于 1 即缓降） */
    public static final IAttribute FALL_SPEED = create("eaddons.fallSpeed", 1, 0, 4);
    /** 跳跃倍率：起跳竖直速度 × 值 */
    public static final IAttribute JUMP_BOOST = create("eaddons.jumpBoost", 1, 0, 16);
    /** 暴击伤害：暴击倍率额外 + 值 */
    public static final IAttribute CRIT_DAMAGE = create("eaddons.critDamage", 0, 0, 64);
    /** 摔伤免疫阈值：单次摔伤不超过此值时直接免疫 */
    public static final IAttribute FALL_IMMUNITY = create("eaddons.fallImmunity", 0, 0, 1024);

    public static final List<IAttribute> ALL = Collections.unmodifiableList(Arrays.asList(
            LIFESTEAL, PROJECTILE_DEFLECT, ETHERIUM_SHIELD, MINING_SPEED,
            FALL_SPEED, JUMP_BOOST, CRIT_DAMAGE, FALL_IMMUNITY));

    private EnigmaticAttributes() {
    }

    private static IAttribute create(String name, double def, double min, double max) {
        // 全部同步到客户端：移动类与挖掘速度由客户端参与计算，其余用于提示显示
        return new RangedAttribute(null, name, def, min, max).setShouldWatch(true);
    }

    /** 读取实体的属性值；实体没有该属性时返回默认值 */
    public static double get(EntityLivingBase entity, IAttribute attribute) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance == null ? attribute.getDefaultValue() : instance.getAttributeValue();
    }

    /**
     * 玩家构造时注册全部属性（两端都会触发）。必须先判重，重复注册会抛异常。
     * <p>只给玩家：来源全是玩家的饰品/手持物，给所有生物注册会让每个实体存档多出 8 条属性。
     * 生物没有这些属性时 {@link #get} 返回默认值，结算处理器照常跳过。
     */
    public static final class Registrar {
        @SubscribeEvent
        public void onEntityConstructing(EntityEvent.EntityConstructing event) {
            if (!(event.getEntity() instanceof EntityPlayer)) {
                return;
            }
            AbstractAttributeMap map = ((EntityPlayer) event.getEntity()).getAttributeMap();
            for (IAttribute attribute : ALL) {
                if (map.getAttributeInstance(attribute) == null) {
                    map.registerAttribute(attribute);
                }
            }
        }
    }
}
