package net.mx.eaddons.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.mx.eaddons.util.PotionRefresh;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.*;

public class ItemArtificialFlower extends Item {
    public static final ItemArtificialFlower INSTANCE = new ItemArtificialFlower();

    /** 给予效果的时长（tick）。原先 36 tick 太短，稍有卡顿就会断档、看着像瞬间消失。 */
    private static final int EFFECT_DURATION = 100;
    /** 剩余时长不超过此值才续期，避免每 tick 重发效果包。 */
    private static final int EFFECT_REFRESH_AT = 4;

    public ItemArtificialFlower() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("artificial_flower");
        setRegistryName("artificial_flower");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean("FlowerEnable");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean("FlowerEnable", false);
        player.getCooldownTracker().setCooldown(this, 30);
        if (!world.isRemote) {
            player.openGui(EAddonsMod.instance, AntiqueBagGuiHandler.FLOWER_GUI_ID, world,
                    (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** 佩戴者是否装备了神秘遗物的「非欧立方」。 */
    private static boolean hasTheCube(EntityPlayer player) {
        if (player == null) return false;
        Item cube = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "the_cube"));
        return cube != null && baubles.api.BaublesApi.isBaubleEquipped(player, cube) != -1;
    }

    /**
     * 石英花提供效果的等级（1 起算，对应 amplifier = 等级-1）。
     * 花内放入魔法石英戒指 +1；佩戴非欧立方再 +{@link ArtificialFlowerConfig#theCubeBonusLevel}。
     */
    private static int getEffectLevel(NBTTagCompound tag, EntityPlayer player) {
        int level = 1;
        if (tag != null && tag.hasKey("MagicRing")) level++;
        if (hasTheCube(player)) level += ArtificialFlowerConfig.theCubeBonusLevel;
        return level;
    }

    /** 1..5 的罗马数字，用于 tooltip 显示等级。 */
    private static String toRoman(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return Integer.toString(level);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.artificial_flower.attribute_header"));
        int attrCount = 0;
        for (int id = 1; id <= 3; id++) {
            Helper.AttributeData data = Helper.getAttribute(stack, id);
            if (data != null) {
                list.add(GuiArtificialFlower.getAttributeText(data));
                attrCount++;
            }
        }
        if (attrCount == 0) {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.artificial_flower.none"));
        }

        list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.artificial_flower.effect_header"));
        int effectCount = 0;
        NBTTagCompound tag = stack.getTagCompound();
        String levelText = " " + toRoman(getEffectLevel(tag, net.minecraft.client.Minecraft.getMinecraft().player));
        for (int id = 0; id < 2; id++) {
            Potion effect = Helper.getEffect(stack, id);
            if (effect == null)
                continue;
            effectCount++;
            String name = I18n.format(effect.getName());
            TextFormatting color = effect.isBeneficial() ? TextFormatting.GREEN : TextFormatting.RED;
            if (id == 0) {
                list.add(color + I18n.format("tooltip.eaddons.artificial_flower.providing", name + levelText));
            } else {
                list.add(color + I18n.format("tooltip.eaddons.artificial_flower.immunity", name));
            }
        }
        if (effectCount == 0) {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.artificial_flower.none"));
        }
        list.add("");
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof EntityPlayer))
            return;
        EntityPlayer player = (EntityPlayer) entity;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return;

        if (player instanceof EntityPlayerMP) {
            UUID flowerEnableUUID = Helper.getPlayerEnableUUID(player);
            UUID flowerUUID = Helper.getFlowerUUID(stack);
            if ((flowerUUID == null || !flowerUUID.equals(flowerEnableUUID)) && tag.getBoolean("FlowerEnable")) {
                tag.setBoolean("FlowerEnable", false);
                for (int i = 1; i <= 3; i++) {
                    Helper.AttributeData data = Helper.getAttribute(stack, i);
                    if (data != null) {
                        IAttributeInstance inst = player.getAttributeMap()
                                .getAttributeInstanceByName(data.attributeName);
                        if (inst != null) {
                            inst.removeModifier(data.modifier);
                        }
                    }
                }
            }
        }

        boolean bagFlag = false;
        if (tag.hasKey("FlowerBagEnable")) {
            int bagSlot = tag.getInteger("FlowerBagEnable");
            if (ItemAntiqueBag.hasBag(player)) {
                List<ItemStack> inv = ItemAntiqueBag.getInventory(player);
                if (bagSlot >= 0 && bagSlot < inv.size()) {
                    ItemStack bagStack = inv.get(bagSlot);
                    UUID bagUUID = Helper.getFlowerUUID(bagStack);
                    UUID thisUUID = Helper.getFlowerUUID(stack);
                    bagFlag = bagUUID != null && bagUUID.equals(thisUUID);
                }
            }
        }

        if (!tag.getBoolean("FlowerEnable") && !bagFlag)
            return;

        if (Helper.attributePool == null) {
            Helper.initRandomPool(player);
        }

        for (int i = 1; i <= 3; i++) {
            Helper.AttributeData data = Helper.getAttribute(stack, i);
            if (data != null && ArtificialFlowerConfig.isAttributeBlacklisted(data.attributeName)
                    && !Helper.isCoreChosen(stack, false, i)) {
                Helper.removeAttribute(stack, i);
            }
        }
        for (int i = 0; i < 2; i++) {
            Potion effect = Helper.getEffect(stack, i);
            if (effect != null) {
                ResourceLocation effectId = effect.getRegistryName();
                if (effectId != null && !Helper.isCoreChosen(stack, true, i)
                        && (ArtificialFlowerConfig.isEffectBlacklisted(effectId)
                        || i == 1 && ArtificialFlowerConfig.isImmunityBlacklisted(effectId))) {
                    Helper.removeEffect(stack, i);
                }
            }
        }

        Multimap<String, AttributeModifier> attrMap = HashMultimap.create();
        for (int i = 1; i <= 3; i++) {
            Helper.AttributeData data = Helper.getAttribute(stack, i);
            if (data != null) {
                attrMap.put(data.attributeName, data.modifier);
            }
        }
        if (player instanceof EntityPlayerMP && !attrMap.isEmpty()) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            Map<Multimap<String, AttributeModifier>, Integer> tickMap = ArtificialFlowerEventHandler.PLAYER_ATTRIBUTE_MAP
                    .computeIfAbsent(serverPlayer, k -> new HashMap<>());
            if (tickMap.containsKey(attrMap)) {
                tickMap.put(attrMap, 3);
            } else {
                tickMap.put(attrMap, 3);
            }
        }

        Potion effectImmuneTo = Helper.getEffect(stack, 1);
        if (effectImmuneTo != null && player.isPotionActive(effectImmuneTo)) {
            player.removePotionEffect(effectImmuneTo);
        }

        Potion effectProvided = Helper.getEffect(stack, 0);
        // amplifier = 等级-1；等级含戒指与非欧立方加成
        int amplifier = getEffectLevel(tag, player) - 1;
        if (effectProvided != null) {
            if (effectProvided.isInstant()) {
                if (player.ticksExisted % 100 == 0) {
                    double modifier = ArtificialFlowerConfig.randomInstantaneousEffectModifier / 100.0;
                    effectProvided.affectEntity(player, player, player, amplifier, modifier);
                }
            } else if (!player.world.isRemote) {
                // 不能「先移除再施加」：那样每续一次，伤害吸收就补满一次、生命提升就掉一截心
                PotionRefresh.ensure(player, effectProvided, amplifier, EFFECT_DURATION, EFFECT_REFRESH_AT, true, true);
            }
        }
    }

    public static class Helper {

        private static final String CORE_ATTRIBUTE = "CoreChosenAttribute";
        private static final String CORE_EFFECT = "CoreChosenEffect";

        public static List<IAttribute> attributePool;
        public static List<Potion> potionEffectPool;
        public static List<Potion> allEffectPool;

        public static void initRandomPool(EntityPlayer player) {
            List<IAttribute> attrBuilder = new ArrayList<>();
            for (IAttributeInstance inst : player.getAttributeMap().getAllAttributes()) {
                IAttribute attr = inst.getAttribute();
                if (!ArtificialFlowerConfig.isAttributeBlacklisted(attr.getName())) {
                    attrBuilder.add(attr);
                }
            }
            attributePool = Collections.unmodifiableList(attrBuilder);

            Set<Potion> potionEffects = new LinkedHashSet<>();
            for (PotionType type : ForgeRegistries.POTION_TYPES.getValuesCollection()) {
                for (PotionEffect pe : type.getEffects()) {
                    potionEffects.add(pe.getPotion());
                }
            }

            List<Potion> potionBuilder = new ArrayList<>();
            for (Potion p : potionEffects) {
                ResourceLocation id = p.getRegistryName();
                if (id == null)
                    continue;
                if (ArtificialFlowerConfig.isEffectBlacklisted(id))
                    continue;
                if (id.toString().contains("flight") || id.toString().contains("fly"))
                    continue;
                potionBuilder.add(p);
            }
            potionEffectPool = Collections.unmodifiableList(potionBuilder);

            List<Potion> allBuilder = new ArrayList<>();
            for (Potion p : ForgeRegistries.POTIONS.getValuesCollection()) {
                ResourceLocation id = p.getRegistryName();
                if (id == null)
                    continue;
                if (ArtificialFlowerConfig.isEffectBlacklisted(id))
                    continue;
                if (id.toString().contains("flight") || id.toString().contains("fly"))
                    continue;
                allBuilder.add(p);
            }
            allEffectPool = Collections.unmodifiableList(allBuilder);
        }

        public static class AttributeData {
            public final String attributeName;
            public final AttributeModifier modifier;

            public AttributeData(String attributeName, AttributeModifier modifier) {
                this.attributeName = attributeName;
                this.modifier = modifier;
            }
        }

        public static void setAttribute(ItemStack stack, int index, IAttribute attribute, AttributeModifier modifier) {
            NBTTagCompound tag = getOrCreateTag(stack);
            tag.setString("AttributeId" + index, attribute.getName());
            tag.setTag("AttributeModifier" + index, SharedMonsterAttributes.writeAttributeModifierToNBT(modifier));
            tag.removeTag(CORE_ATTRIBUTE + index);
        }

        public static void removeAttribute(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return;
            tag.removeTag("AttributeId" + index);
            tag.removeTag("AttributeModifier" + index);
            tag.removeTag(CORE_ATTRIBUTE + index);
        }

        public static void setEffect(ItemStack stack, int index, Potion effect) {
            NBTTagCompound tag = getOrCreateTag(stack);
            ResourceLocation id = effect.getRegistryName();
            if (id != null) {
                tag.setString("PotionEffect" + index, id.toString());
            }
            tag.removeTag(CORE_EFFECT + index);
        }

        public static void removeEffect(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return;
            tag.removeTag("PotionEffect" + index);
            tag.removeTag(CORE_EFFECT + index);
        }

        /** 术质核心自选写入的词条打上标记：无视各黑名单（不被清理、免疫照常生效）；普通洗练或移除时标记随之清掉。 */
        public static void markCoreChosen(ItemStack stack, boolean effect, int index) {
            getOrCreateTag(stack).setBoolean((effect ? CORE_EFFECT : CORE_ATTRIBUTE) + index, true);
        }

        public static boolean isCoreChosen(ItemStack stack, boolean effect, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null && tag.getBoolean((effect ? CORE_EFFECT : CORE_ATTRIBUTE) + index);
        }

        @Nullable
        public static AttributeData getAttribute(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return null;
            String idKey = "AttributeId" + index;
            String modKey = "AttributeModifier" + index;
            if (!tag.hasKey(idKey, 8) || !tag.hasKey(modKey, 10))
                return null;
            String attrName = tag.getString(idKey);
            AttributeModifier modifier = SharedMonsterAttributes
                    .readAttributeModifierFromNBT(tag.getCompoundTag(modKey));
            if (modifier == null) {
                removeAttribute(stack, index);
                return null;
            }
            return new AttributeData(attrName, modifier);
        }

        @Nullable
        public static Potion getEffect(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return null;
            String key = "PotionEffect" + index;
            if (!tag.hasKey(key, 8))
                return null;
            Potion effect = Potion.getPotionFromResourceLocation(tag.getString(key));
            if (effect == null) {
                removeEffect(stack, index);
                return null;
            }
            return effect;
        }

        /**
         * 洗练第 index（1~3）条属性，数值在 ±maxPercent% 内按正态分布取；另外两条已有的属性不会抽到（同一朵花不重复）。
         * 没有可抽的属性时返回 false（调用方不扣材料）。
         */
        public static boolean randomAttribute(EntityPlayer player, ItemStack stack, int index, int costMode,
                boolean boost, int maxPercent) {
            if (attributePool == null || attributePool.isEmpty()) {
                initRandomPool(player);
            }
            Set<String> others = new HashSet<>();
            for (int i = 1; i <= 3; i++) {
                AttributeData data = i == index ? null : getAttribute(stack, i);
                if (data != null) {
                    others.add(data.attributeName);
                }
            }
            List<IAttribute> candidates = new ArrayList<>();
            for (IAttribute attr : attributePool) {
                if (!others.contains(attr.getName())) {
                    candidates.add(attr);
                }
            }
            if (candidates.isEmpty())
                return false;

            Random rand = player.getRNG();
            IAttribute attribute = candidates.get(rand.nextInt(candidates.size()));
            double offset = (costMode == 0 ? 0 : costMode == 1 ? 0.3 : 0.6) - (boost ? 0 : 0.125);
            double gaussian = MathHelper.clamp(rand.nextGaussian() + offset, -2.5, 2.5);
            double value = 0.01 * (int) (gaussian / 2.5 * maxPercent);

            AttributeData oldData = getAttribute(stack, index);
            if (oldData != null) {
                IAttributeInstance inst = player.getAttributeMap().getAttributeInstanceByName(oldData.attributeName);
                if (inst != null) {
                    inst.removeModifier(oldData.modifier);
                }
            }

            if (value == 0) {
                removeAttribute(stack, index);
            } else {
                AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(), "ArtificialFlower" + index, value, 1);
                setAttribute(stack, index, attribute, modifier);
            }
            return true;
        }

        /**
         * 洗练效果：index 0 常驻、1 免疫。免疫槽照旧从全部效果抽；常驻槽用石英只抽基础名单，
         * 用邪恶精髓（essence）按原规则抽、但排除基础名单。没有可抽的效果时返回 false（调用方不扣材料）。
         */
        public static boolean randomEffect(EntityPlayer player, ItemStack stack, int index, boolean essence) {
            if (allEffectPool == null || potionEffectPool == null) {
                initRandomPool(player);
            }
            Random rand = player.getRNG();
            Potion otherEffect = getEffect(stack, 1 - index);
            Potion effect;
            if (index == 0 && !essence) {
                effect = pick(rand, basicProvidedPool(), otherEffect, false, false);
            } else {
                NBTTagCompound tag = getOrCreateTag(stack);
                int count = tag.hasKey("AllEffectCount") ? tag.getInteger("AllEffectCount") : 1;
                boolean fromAll = index == 1 || potionEffectPool.isEmpty() || rand.nextInt((count + 1) / 2 + 1) == 0;
                boolean excludeBasic = index == 0;
                boolean immunity = index == 1;
                effect = pick(rand, fromAll ? allEffectPool : potionEffectPool, otherEffect, excludeBasic, immunity);
                if (effect == null && !fromAll) {
                    fromAll = true;
                    effect = pick(rand, allEffectPool, otherEffect, excludeBasic, immunity);
                }
                if (effect != null && fromAll) {
                    tag.setInteger("AllEffectCount", count + 1);
                }
            }
            if (effect == null)
                return false;

            Potion oldEffect = getEffect(stack, index);
            if (oldEffect != null && player.isPotionActive(oldEffect)) {
                player.removePotionEffect(oldEffect);
            }
            setEffect(stack, index, effect);
            return true;
        }

        /** 从 pool 里随机取一个：不与另一槽重复，excludeBasic 时跳过基础名单，immunity 时跳过免疫黑名单。 */
        @Nullable
        private static Potion pick(Random rand, List<Potion> pool, @Nullable Potion other, boolean excludeBasic,
                boolean immunity) {
            List<Potion> candidates = new ArrayList<>();
            for (Potion potion : pool) {
                ResourceLocation id = potion.getRegistryName();
                if (potion != other && !(excludeBasic && ArtificialFlowerConfig.isBasicProvidedEffect(id))
                        && !(immunity && ArtificialFlowerConfig.isImmunityBlacklisted(id))) {
                    candidates.add(potion);
                }
            }
            return candidates.isEmpty() ? null : candidates.get(rand.nextInt(candidates.size()));
        }

        /** 基础名单里实际注册、且没被黑名单排除的效果。 */
        private static List<Potion> basicProvidedPool() {
            List<Potion> pool = new ArrayList<>();
            for (ResourceLocation id : ArtificialFlowerConfig.basicProvidedEffects()) {
                Potion potion = ForgeRegistries.POTIONS.getValue(id);
                if (potion != null && !ArtificialFlowerConfig.isEffectBlacklisted(id)) {
                    pool.add(potion);
                }
            }
            return pool;
        }

        public static ItemStack getFlowerStack(EntityPlayer player, boolean copy) {
            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack flower;
            if (mainHand.getItem() instanceof ItemArtificialFlower) {
                flower = mainHand;
            } else {
                flower = player.getHeldItemOffhand();
            }
            return copy ? flower.copy() : flower;
        }

        @Nullable
        public static UUID getFlowerUUID(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null || !tag.hasUniqueId("FlowerUUID"))
                return null;
            return tag.getUniqueId("FlowerUUID");
        }

        @Nullable
        public static UUID getPlayerEnableUUID(EntityPlayer player) {
            NBTTagCompound data = player.getEntityData();
            if (!data.hasKey("PlayerPersisted"))
                return null;
            NBTTagCompound persisted = data.getCompoundTag("PlayerPersisted");
            if (!persisted.hasUniqueId("FlowerEnableUUID"))
                return null;
            return persisted.getUniqueId("FlowerEnableUUID");
        }

        public static void setPlayerEnableUUID(EntityPlayer player, UUID uuid) {
            NBTTagCompound data = player.getEntityData();
            NBTTagCompound persisted;
            if (data.hasKey("PlayerPersisted")) {
                persisted = data.getCompoundTag("PlayerPersisted");
            } else {
                persisted = new NBTTagCompound();
                data.setTag("PlayerPersisted", persisted);
            }
            persisted.setUniqueId("FlowerEnableUUID", uuid);
        }

        private static NBTTagCompound getOrCreateTag(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            return tag;
        }
    }
}
