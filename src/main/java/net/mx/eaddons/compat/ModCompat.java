package net.mx.eaddons.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

/**
 * 跨模组兼容开关。在 preInit 阶段检测相关模组是否加载，供其它代码在调用兼容类前做守卫，
 * 从而在对应模组缺失时不触碰其类（避免 NoClassDefFound）。
 */
public final class ModCompat {
    public static boolean SIMPLE_DIFFICULTY = false;
    public static boolean MOBENDS = false;
    public static boolean ICE_AND_FIRE = false;
    public static boolean FIRST_AID = false;

    private ModCompat() {
    }

    public static void init() {
        SIMPLE_DIFFICULTY = Loader.isModLoaded("simpledifficulty");
        MOBENDS = Loader.isModLoaded("mobends");
        ICE_AND_FIRE = Loader.isModLoaded("iceandfire");
        FIRST_AID = Loader.isModLoaded("firstaid");
    }

    /**
     * First Aid 在 LivingHurtEvent 的 LOWEST 接管玩家伤害后会取消该事件，玩家身上不会再触发 LivingDamageEvent。
     * 原本挂在 LivingDamageEvent 上的「玩家受伤」逻辑，此时要另在 LivingHurtEvent 里补一条路径。
     */
    public static boolean firstAidTakesOver(EntityLivingBase victim) {
        return FIRST_AID && victim instanceof EntityPlayer;
    }
}
