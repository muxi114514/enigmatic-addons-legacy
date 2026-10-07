package net.mx.eaddons.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.Capability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 读 First Aid 的身体部位血量。走反射，免去编译期依赖；只读不写。
 */
public final class FirstAidHealth {
    private static final Logger LOG = LogManager.getLogger("eaddons");

    private static boolean initialized;
    private static Capability<?> capability;
    private static Field canCauseDeath;
    private static Field currentHealth;
    private static Method getMaxHealth;

    private FirstAidHealth() {
    }

    /**
     * 能致死的部位（默认头部与躯干）中最低的「当前 / 上限」比例。
     * 没装 First Aid、取不到数据、或配置里没有致死部位时返回 null，调用方改用原版生命。
     */
    @Nullable
    public static synchronized Float lowestCriticalRatio(EntityPlayer player) {
        if (!ModCompat.FIRST_AID || !init()) {
            return null;
        }
        Object model = player.getCapability(capability, null);
        if (!(model instanceof Iterable)) {
            return null;
        }
        try {
            float lowest = Float.MAX_VALUE;
            for (Object part : (Iterable<?>) model) {
                int max = (Integer) getMaxHealth.invoke(part);
                if (max > 0 && canCauseDeath.getBoolean(part)) {
                    lowest = Math.min(lowest, currentHealth.getFloat(part) / max);
                }
            }
            return lowest == Float.MAX_VALUE ? null : lowest;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("[FirstAid] Reading body part health failed, using vanilla health instead", e);
            return null;
        }
    }

    private static boolean init() {
        if (initialized) {
            return capability != null;
        }
        initialized = true;
        try {
            capability = (Capability<?>) Class.forName("ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem")
                    .getField("INSTANCE").get(null);
            Class<?> part = Class.forName("ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart");
            canCauseDeath = part.getField("canCauseDeath");
            currentHealth = part.getField("currentHealth");
            getMaxHealth = part.getMethod("getMaxHealth");
        } catch (ReflectiveOperationException | RuntimeException e) {
            capability = null;
            LOG.error("[FirstAid] Body part access unavailable, using vanilla health instead", e);
        }
        return capability != null;
    }
}
