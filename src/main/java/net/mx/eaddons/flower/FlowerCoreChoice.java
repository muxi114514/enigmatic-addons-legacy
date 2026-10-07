package net.mx.eaddons.flower;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.item.ArtificialFlowerConfig;
import net.mx.eaddons.item.ContainerArtificialFlower;
import net.mx.eaddons.item.ItemArtificialFlower;
import net.mx.eaddons.item.ItemArtificialFlower.Helper;
import net.mx.eaddons.item.ItemSpellcore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 石英花的术质核心自选：青金石栏放核心可自选一条满值属性，石英栏放核心可自选常驻 / 免疫效果，各消耗 1 个核心。
 * 候选列表客户端用来显示、服务端用来校验，两边同一套规则。
 */
public final class FlowerCoreChoice {
    /** 界面材料栏：0 青金石栏（属性），1 石英栏（效果）。 */
    public static final int ATTRIBUTE_MATERIAL = 0;
    public static final int EFFECT_MATERIAL = 1;
    /** 效果槽：0 常驻，1 免疫。 */
    public static final int PROVIDE = 0;
    public static final int IMMUNITY = 1;

    private static final double EPSILON = 1.0E-9;
    /** 有药水形态的效果（常驻槽的来源，与随机洗练的主池一致）；注册表在运行期不变，首次用到时建好。 */
    private static volatile Set<Potion> potionTypeEffects;

    private FlowerCoreChoice() {
    }

    public static boolean isCore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ItemSpellcore.INSTANCE;
    }

    /** 属性满值，与随机洗练的上限同一配置（RandomAttributeMaxModifier%）。 */
    public static double maxAttributeValue() {
        return 0.01 * ArtificialFlowerConfig.randomAttributeMaxModifier;
    }

    /** 第 index（1~3）条可选的属性：无视属性黑名单（只认自选专用黑名单），排除其他两条已有的，以及本条已经满值的同一属性。 */
    public static List<IAttribute> attributeChoices(EntityPlayer player, ItemStack flower, int index) {
        List<IAttribute> result = new ArrayList<>();
        if (index < 1 || index > 3 || maxAttributeValue() <= 0) {
            return result;
        }
        Set<String> others = new HashSet<>();
        Helper.AttributeData current = null;
        for (int i = 1; i <= 3; i++) {
            Helper.AttributeData data = Helper.getAttribute(flower, i);
            if (data == null) {
                continue;
            }
            if (i == index) {
                current = data;
            } else {
                others.add(data.attributeName);
            }
        }
        for (IAttributeInstance instance : player.getAttributeMap().getAllAttributes()) {
            String name = instance.getAttribute().getName();
            if (others.contains(name) || ArtificialFlowerConfig.isChoiceAttributeBlacklisted(name)) {
                continue;
            }
            if (current != null && current.attributeName.equals(name)
                    && current.modifier.getAmount() >= maxAttributeValue() - EPSILON) {
                continue;
            }
            result.add(instance.getAttribute());
        }
        return result;
    }

    /** 效果槽可选的效果：常驻只列有药水形态的正面效果，免疫只列负面效果；无视效果黑名单与免疫黑名单（只认自选专用黑名单），排除飞行类、两个槽已有的。 */
    public static List<Potion> effectChoices(ItemStack flower, int index) {
        List<Potion> result = new ArrayList<>();
        if (index != PROVIDE && index != IMMUNITY) {
            return result;
        }
        Potion current = Helper.getEffect(flower, index);
        Potion other = Helper.getEffect(flower, 1 - index);
        Collection<Potion> source = index == PROVIDE ? potionTypeEffects() : ForgeRegistries.POTIONS.getValuesCollection();
        for (Potion potion : source) {
            ResourceLocation id = potion.getRegistryName();
            if (id == null || potion == current || potion == other || potion.isBadEffect() != (index == IMMUNITY)) {
                continue;
            }
            String text = id.toString();
            // 与随机池同一条规则：飞行类效果不进石英花
            if (text.contains("flight") || text.contains("fly")) {
                continue;
            }
            if (ArtificialFlowerConfig.isChoiceEffectBlacklisted(id)) {
                continue;
            }
            result.add(potion);
        }
        return result;
    }

    /**
     * 服务端：校验界面、手中的花与对应栏里的核心，选项必须在候选列表内，成功后写入词条并扣 1 个核心。
     * effect 为 false 时 index 是属性条 1~3，为 true 时是效果槽 0/1。
     */
    public static void apply(EntityPlayerMP player, int windowId, boolean effect, int index, String id) {
        if (!(player.openContainer instanceof ContainerArtificialFlower) || player.openContainer.windowId != windowId) {
            return;
        }
        ContainerArtificialFlower container = (ContainerArtificialFlower) player.openContainer;
        int material = effect ? EFFECT_MATERIAL : ATTRIBUTE_MATERIAL;
        if (!container.hasCore(material)) {
            return;
        }
        ItemStack flower = Helper.getFlowerStack(player, false);
        if (!(flower.getItem() instanceof ItemArtificialFlower)) {
            return;
        }
        boolean applied = effect ? applyEffect(player, flower, index, id) : applyAttribute(player, flower, index, id);
        if (applied) {
            container.consumeCore(material);
            player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS, 0.8F, 1.2F);
        }
    }

    private static boolean applyAttribute(EntityPlayerMP player, ItemStack flower, int index, String name) {
        for (IAttribute attribute : attributeChoices(player, flower, index)) {
            if (!attribute.getName().equals(name)) {
                continue;
            }
            // 与随机洗练相同：先从玩家身上摘掉旧修饰符，花重新启用时再挂新的
            Helper.AttributeData old = Helper.getAttribute(flower, index);
            if (old != null) {
                IAttributeInstance instance = player.getAttributeMap().getAttributeInstanceByName(old.attributeName);
                if (instance != null) {
                    instance.removeModifier(old.modifier);
                }
            }
            Helper.setAttribute(flower, index, attribute,
                    new AttributeModifier(UUID.randomUUID(), "ArtificialFlower" + index, maxAttributeValue(), 1));
            Helper.markCoreChosen(flower, false, index);
            return true;
        }
        return false;
    }

    private static boolean applyEffect(EntityPlayerMP player, ItemStack flower, int index, String id) {
        for (Potion potion : effectChoices(flower, index)) {
            ResourceLocation potionId = potion.getRegistryName();
            if (potionId == null || !potionId.toString().equals(id)) {
                continue;
            }
            Potion old = Helper.getEffect(flower, index);
            if (old != null && player.isPotionActive(old)) {
                player.removePotionEffect(old);
            }
            Helper.setEffect(flower, index, potion);
            Helper.markCoreChosen(flower, true, index);
            return true;
        }
        return false;
    }

    private static Set<Potion> potionTypeEffects() {
        Set<Potion> cached = potionTypeEffects;
        if (cached == null) {
            Set<Potion> set = new LinkedHashSet<>();
            for (PotionType type : ForgeRegistries.POTION_TYPES.getValuesCollection()) {
                for (PotionEffect effect : type.getEffects()) {
                    set.add(effect.getPotion());
                }
            }
            cached = Collections.unmodifiableSet(set);
            potionTypeEffects = cached;
        }
        return cached;
    }
}
