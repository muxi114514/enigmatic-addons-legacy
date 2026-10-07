package net.mx.eaddons.item;

import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 受诅咒/受祝福饰品的佩戴判定，统一遵循 EnigmaticLegacy 的 ItemBeCursed / ItemBeBlessed 配置：
 * 若物品在受诅咒表且玩家非受诅咒者，则仅当"受祝福者 且 物品也在受祝福表"时可佩戴，否则禁止；
 * 若物品不在任何表中，则任何玩家均可佩戴。供本模组的 IBauble 饰品复用。
 */
public final class CursedEquipHelper {
    private CursedEquipHelper() {
    }

    public static boolean canEquip(EntityLivingBase entity, ItemStack stack) {
        if (!(entity instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.capabilities.isCreativeMode) return true;
        if (SuperpositionHandler.isCursed(stack) && !SuperpositionHandler.hasCursed(player)) {
            return SuperpositionHandler.hasBlessed(player) && SuperpositionHandler.isBlessed(stack);
        }
        return true;
    }
}
