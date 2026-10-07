package net.mx.eaddons.attribute;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/**
 * 修饰符对账：让指定 UUID 的修饰符等于目标值。
 * <p>数值不变时不动它——改修饰符会把属性标脏并整包同步，每 tick 重挂会刷网络包。
 * 修饰符不写入存档，由来源每隔几 tick 重新计算，登录、复活、换维度后自动恢复。
 */
public final class ModifierSync {

    private ModifierSync() {
    }

    /**
     * @param operation 0 = 加值，1 = 乘基础值，2 = 乘总值（1.12 原版语义）
     */
    public static void apply(EntityLivingBase entity, IAttribute attribute, UUID id, String name,
                             double amount, int operation) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount && current.getOperation() == operation) {
            return;
        }
        if (current != null) {
            instance.removeModifier(id);
        }
        if (amount != 0) {
            instance.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
        }
    }
}
