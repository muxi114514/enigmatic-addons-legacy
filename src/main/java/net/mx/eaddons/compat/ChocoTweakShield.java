package net.mx.eaddons.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * chocotweak「剑盾组合」的软依赖。合成时它只在剑上写一个整数标签 {@code Shield}（盾牌 meta），
 * 格挡后的体力、盾牌效果与觉醒都由它自己按「正在格挡的物品带该标签」结算，这里只需读标签。
 * 疲倦按药水注册名查；「提灯」觉醒（副手有东西也能举盾）反射调用它的公开 API，模组不在时一律当作没有。
 */
public final class ChocoTweakShield {

    private static final Logger LOG = LogManager.getLogger("eaddons");
    private static final String NBT_SHIELD = "Shield";
    private static final ResourceLocation EXHAUSTION_ID = new ResourceLocation("chocotweak", "exhaustion");

    private static volatile Potion exhaustion;
    private static volatile boolean exhaustionResolved;
    private static volatile Method dualWieldBlock;
    private static volatile boolean dualWieldResolved;

    private ChocoTweakShield() {
    }

    public static boolean hasShield(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(NBT_SHIELD);
    }

    /** 疲倦（体力耗尽）期间 chocotweak 不让举盾，这里保持一致。 */
    public static boolean isExhausted(EntityLivingBase entity) {
        if (!exhaustionResolved) {
            exhaustion = ForgeRegistries.POTIONS.getValue(EXHAUSTION_ID);
            exhaustionResolved = true;
        }
        Potion potion = exhaustion;
        return potion != null && entity.isPotionActive(potion);
    }

    /** 剑盾是否有「提灯」觉醒：副手拿着东西也能举盾。 */
    public static boolean canDualWieldBlock(ItemStack stack) {
        if (!dualWieldResolved) {
            if (Loader.isModLoaded("chocotweak")) {
                try {
                    dualWieldBlock = Class.forName("com.chocotweak.api.BStarSwordShieldAPI")
                            .getMethod("canDualWieldBlock", ItemStack.class);
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOG.warn("chocotweak sword-shield API not found, dual-wield blocking disabled", e);
                }
            }
            dualWieldResolved = true;
        }
        Method method = dualWieldBlock;
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, stack));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
