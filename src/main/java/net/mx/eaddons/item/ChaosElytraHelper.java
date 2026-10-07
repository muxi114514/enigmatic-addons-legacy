package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * 混沌鞘翅的共享工具：
 * 1) 通过反射调用 {@code Entity#setFlag(7, ...)} 手动开/关鞘翅飞行状态——
 *    1.12.2 的鞘翅飞行由 {@code isElytraFlying()}（读第 7 位标志）驱动，与胸甲物品无关，
 *    落地时原版 {@code travel()} 会自动清除该标志，故只需负责"开启"。
 * 2) 从 Baubles 饰品栏查找已佩戴的混沌鞘翅。
 */
public final class ChaosElytraHelper {
    private static final Method SET_FLAG =
            ReflectionHelper.findMethod(Entity.class, "setFlag", "func_70052_a", int.class, boolean.class);
    private static final int FLAG_ELYTRA = 7;

    private ChaosElytraHelper() {
    }

    public static void setElytraFlying(EntityLivingBase entity, boolean flying) {
        try {
            SET_FLAG.invoke(entity, FLAG_ELYTRA, flying);
        } catch (Exception ignored) {
        }
    }

    /** 返回玩家佩戴的混沌鞘翅物品栈，未佩戴返回 EMPTY。 */
    public static ItemStack getChaosElytra(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemChaosElytra) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isWorn(EntityPlayer player) {
        return !getChaosElytra(player).isEmpty();
    }
}
