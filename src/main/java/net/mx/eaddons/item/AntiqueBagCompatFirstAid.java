package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility handler for First Aid mod when half_heart_mask effect is active.
 * Uses reflection to avoid compile-time dependency on First Aid.
 * Caps each body part's health at 50% of its maximum, preventing head-drain deaths.
 */
public class AntiqueBagCompatFirstAid {

    private static final Logger LOG = LogManager.getLogger("eaddons");

    private static boolean initialized = false;
    private static boolean available = false;

    private static Object capabilityInstance;
    private static Method getFromEnumMethod;
    private static Method getMaxHealthMethod;
    private static Object[] playerPartValues;
    private static Field currentHealthField;

    private static Object networking;
    private static java.lang.reflect.Constructor<?> syncMessageConstructor;
    private static Method sendToMethod;
    private static Method scheduleResyncMethod;

    private static void initialize() {
        if (initialized)
            return;
        initialized = true;

        try {
            if (!Loader.isModLoaded("firstaid")) {
                available = false;
                return;
            }

            Class<?> capClass = Class.forName("ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem");
            Field instanceField = capClass.getField("INSTANCE");
            capabilityInstance = instanceField.get(null);

            Class<?> enumPartClass = Class.forName("ichttt.mods.firstaid.api.enums.EnumPlayerPart");
            Method valuesMethod = enumPartClass.getMethod("values");
            playerPartValues = (Object[]) valuesMethod.invoke(null);

            Class<?> damageModelClass = Class.forName("ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel");
            getFromEnumMethod = damageModelClass.getMethod("getFromEnum", enumPartClass);

            Class<?> damageablePartClass = Class.forName("ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart");
            getMaxHealthMethod = damageablePartClass.getMethod("getMaxHealth");

            // currentHealth may be public or protected depending on First Aid version
            try {
                currentHealthField = damageablePartClass.getField("currentHealth");
            } catch (NoSuchFieldException e) {
                currentHealthField = damageablePartClass.getDeclaredField("currentHealth");
                currentHealthField.setAccessible(true);
            }

            Class<?> firstAidClass = Class.forName("ichttt.mods.firstaid.FirstAid");
            Field networkingField = firstAidClass.getField("NETWORKING");
            networking = networkingField.get(null);

            Class<?> messageClass = Class.forName("ichttt.mods.firstaid.common.network.MessageSyncDamageModel");
            syncMessageConstructor = messageClass.getConstructor(damageModelClass, boolean.class);

            scheduleResyncMethod = damageModelClass.getMethod("scheduleResync");

            sendToMethod = networking.getClass().getMethod("sendTo",
                    net.minecraftforge.fml.common.network.simpleimpl.IMessage.class,
                    EntityPlayerMP.class);

            available = true;
            LOG.info("[AntiqueBag] First Aid compat initialized successfully, {} body parts detected", playerPartValues.length);
        } catch (Exception e) {
            available = false;
            LOG.error("[AntiqueBag] First Aid compat initialization FAILED - half_heart_mask per-part capping unavailable", e);
        }
    }

    /**
     * Caps each First Aid body part at 50% of its own max health.
     * This is the ONLY safe way to limit health when First Aid is loaded.
     * Never use player.setHealth() when First Aid is present - it distributes
     * the reduction to body parts and can kill the head instantly.
     *
     * @return true if successfully applied
     */
    public static boolean onFirstAidHlfHealth(EntityPlayerMP player) {
        initialize();
        if (!available)
            return false;

        try {
            Object damageModel = player.getCapability(
                    (net.minecraftforge.common.capabilities.Capability) capabilityInstance, null);
            if (damageModel == null)
                return false;

            boolean changed = false;
            for (Object partEnum : playerPartValues) {
                Object part = getFromEnumMethod.invoke(damageModel, partEnum);
                float curHp = currentHealthField.getFloat(part);
                float maxHp = ((Number) getMaxHealthMethod.invoke(part)).floatValue();
                float cap = maxHp * 0.5F;

                if (curHp > cap) {
                    currentHealthField.setFloat(part, cap);
                    changed = true;
                }
            }

            if (changed) {
                Object message = syncMessageConstructor.newInstance(damageModel, true);
                sendToMethod.invoke(networking, message, player);
            }
            return true;
        } catch (Exception e) {
            LOG.error("[AntiqueBag] First Aid per-part capping failed at runtime", e);
            return false;
        }
    }

    public static boolean isAvailable() {
        initialize();
        return available;
    }

    /**
     * Full-heal all First Aid body parts (head, body, arms, legs, feet).
     * Used by Totem of Malice delayed heal so head and body are not left empty.
     *
     * @return true if First Aid was present and full heal was applied
     */
    public static boolean fullHealFirstAid(EntityPlayerMP player) {
        initialize();
        if (!available)
            return false;

        try {
            Object damageModel = player.getCapability(
                    (net.minecraftforge.common.capabilities.Capability) capabilityInstance, null);
            if (damageModel == null)
                return false;

            for (Object partEnum : playerPartValues) {
                Object part = getFromEnumMethod.invoke(damageModel, partEnum);
                float maxHp = ((Number) getMaxHealthMethod.invoke(part)).floatValue();
                float curHp = currentHealthField.getFloat(part);
                if (curHp < maxHp) {
                    currentHealthField.setFloat(part, maxHp);
                }
            }

            Object message = syncMessageConstructor.newInstance(damageModel, true);
            sendToMethod.invoke(networking, message, player);
            if (scheduleResyncMethod != null) {
                scheduleResyncMethod.invoke(damageModel);
            }
            return true;
        } catch (Exception e) {
            LOG.error("[jmheaven] First Aid full heal failed", e);
            return false;
        }
    }
}
