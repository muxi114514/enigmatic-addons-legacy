package keletu.enigmaticlegacy.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

/**
 * 桥接基类：{@link ItemSpellstoneBauble#fillModifiers} 是 EL 包内的包级私有抽象方法，
 * eaddons 包的子类无法直接覆写。本类置于同一包内实现它，供 eaddons 的咒石饰品继承，
 * 从而复用 EL 的按键分发（getAdvancedBaubles）与免疫/抗性消费（均以 instanceof 硬判定）。
 */
public abstract class EAddonsSpellstoneBauble extends ItemSpellstoneBauble {

    public EAddonsSpellstoneBauble(String props, EnumRarity rare) {
        super(props, rare);
    }

    @Override
    void fillModifiers(Multimap<String, AttributeModifier> attributes, ItemStack stack) {
        // 默认不提供属性修饰符
    }
}
