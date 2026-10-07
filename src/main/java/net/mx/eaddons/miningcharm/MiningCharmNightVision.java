package net.mx.eaddons.miningcharm;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.item.ItemMiningCharm;
import keletu.enigmaticlegacy.util.helper.ItemNBTHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/**
 * 猎宝者护符永久夜视。原版 onWornTick 只在 y&lt;50、不在下界/末地、不在水里、头顶看不到天空且亮度 ≤8 时给夜视，
 * 其余情况移除；这里去掉这些条件，佩戴且右键开关为开时始终给。
 * 时长与给法照原版（每 tick 补到 210 tick），原版的移除方法只移除 ≤209 tick 的夜视，摘下 / 关闭时照样能清掉。
 */
public final class MiningCharmNightVision {
    private static final String ENABLED_TAG = "nightVisionEnabled";

    private MiningCharmNightVision() {
    }

    /** 返回 true 表示已接管本 tick 的夜视处理，原版逻辑不再执行。 */
    public static boolean tick(ItemMiningCharm charm, ItemStack stack, EntityLivingBase living) {
        if (!MiningCharmConfig.permanentNightVision || !(living instanceof EntityPlayer) || living.world.isRemote) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) living;
        // 原版同样要求护符确实在饰品栏里，否则什么也不做
        if (BaublesApi.isBaubleEquipped(player, charm) == -1) {
            return true;
        }
        if (ItemNBTHelper.getBoolean(stack, ENABLED_TAG, true)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, charm.nightVisionDuration, 0, true, false));
        } else {
            charm.removeNightVisionEffect(player, charm.nightVisionDuration);
        }
        return true;
    }
}
