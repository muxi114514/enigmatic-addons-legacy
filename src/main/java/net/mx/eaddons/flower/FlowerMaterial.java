package net.mx.eaddons.flower;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 石英花的高级洗练材料：两个材料栏都接受神秘遗物的邪恶精髓。
 * 属性栏里放它时上限更高，效果栏里放它时常驻效果改抽基础名单以外的（免疫槽不接受）。
 */
public final class FlowerMaterial {

    private static final ResourceLocation EVIL_ESSENCE = new ResourceLocation("enigmaticlegacy", "evil_essence");
    /** 注册表在运行期不变，首次用到时解析。 */
    private static volatile Item essence;

    private FlowerMaterial() {
    }

    public static boolean isEssence(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = essence;
        if (item == null) {
            item = ForgeRegistries.ITEMS.getValue(EVIL_ESSENCE);
            if (item == null || item == Items.AIR) {
                return false;
            }
            essence = item;
        }
        return stack.getItem() == item;
    }
}
