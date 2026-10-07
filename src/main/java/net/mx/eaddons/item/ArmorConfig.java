package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 护甲相关配置。
 */
public class ArmorConfig {
    /** 是否让神秘遗物的以太护甲改为可见（使用本模组的以太色动画贴图）。关闭则恢复原版隐身。 */
    public static boolean visibleEtheriumArmor = true;

    public static void init(Configuration config) {
        visibleEtheriumArmor = config.getBoolean("VisibleEtheriumArmor", "Armor", true,
                "Render Enigmatic Legacy's Etherium Armor with this mod's animated etherium texture "
                        + "instead of being invisible.");
    }
}
