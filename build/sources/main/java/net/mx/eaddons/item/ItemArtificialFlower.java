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

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
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
            boolean hasRing = tag != null && tag.hasKey("MagicRing");
            for (int id = 0; id < 2; id++) {
                Potion effect = Helper.getEffect(stack, id);
                if (effect == null)
                    continue;
                effectCount++;
                String name = I18n.format(effect.getName());
                TextFormatting color = effect.isBeneficial() ? TextFormatting.GREEN : TextFormatting.RED;
                if (id == 0) {
                    String level = hasRing ? " II" : " I";
                    list.add(color + I18n.format("tooltip.eaddons.artificial_flower.providing", name + level));
                } else {
                    list.add(color + I18n.format("tooltip.eaddons.artificial_flower.immunity", name));
                }
            }
            if (effectCount == 0) {
                list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.artificial_flower.none"));
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.artificial_flower.hold_shift"));
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
            if (data != null && ArtificialFlowerConfig.isAttributeBlacklisted(data.attributeName)) {
                Helper.removeAttribute(stack, i);
            }
        }
        for (int i = 0; i < 2; i++) {
            Potion effect = Helper.getEffect(stack, i);
            if (effect != null) {
                ResourceLocation effectId = effect.getRegistryName();
                if (effectId != null && ArtificialFlowerConfig.isEffectBlacklisted(effectId)) {
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
        int amplifier = 0;
        if (tag.hasKey("MagicRing"))
            amplifier++;
        if (effectProvided != null) {
            if (effectProvided.isInstant()) {
                if (player.ticksExisted % 100 == 0) {
                    double modifier = ArtificialFlowerConfig.randomInstantaneousEffectModifier / 100.0;
                    effectProvided.affectEntity(player, player, player, amplifier, modifier);
                }
            } else {
                PotionEffect newInstance = new PotionEffect(effectProvided, 36, amplifier, true, true);
                if (player.isPotionActive(effectProvided)) {
                    PotionEffect existing = player.getActivePotionEffect(effectProvided);
                    if (existing != null) {
                        if (existing.getAmplifier() == amplifier && existing.getDuration() <= 4) {
                            player.removePotionEffect(effectProvided);
                            player.addPotionEffect(newInstance);
                        } else if (existing.getAmplifier() < amplifier) {
                            player.removePotionEffect(effectProvided);
                            player.addPotionEffect(newInstance);
                        }
                    }
                } else {
                    player.addPotionEffect(newInstance);
                }
            }
        }
    }

    public static class Helper {

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
        }

        public static void removeAttribute(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return;
            tag.removeTag("AttributeId" + index);
            tag.removeTag("AttributeModifier" + index);
        }

        public static void setEffect(ItemStack stack, int index, Potion effect) {
            NBTTagCompound tag = getOrCreateTag(stack);
            ResourceLocation id = effect.getRegistryName();
            if (id != null) {
                tag.setString("PotionEffect" + index, id.toString());
            }
        }

        public static void removeEffect(ItemStack stack, int index) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                return;
            tag.removeTag("PotionEffect" + index);
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

        public static void randomAttribute(EntityPlayer player, ItemStack stack, int index, int costMode,
                boolean boost) {
            if (attributePool == null || attributePool.isEmpty()) {
                initRandomPool(player);
            }
            if (attributePool.isEmpty())
                return;

            Random rand = player.getRNG();
            IAttribute attribute = attributePool.get(rand.nextInt(attributePool.size()));
            double offset = (costMode == 0 ? 0 : costMode == 1 ? 0.3 : 0.6) - (boost ? 0 : 0.125);
            double gaussian = MathHelper.clamp(rand.nextGaussian() + offset, -2.5, 2.5);
            double maxMod = ArtificialFlowerConfig.randomAttributeMaxModifier / 100.0;
            double value = 0.01 * (int) (gaussian / 2.5 * (maxMod * 100));

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
        }

        public static void randomEffect(EntityPlayer player, ItemStack stack, int index) {
            if (allEffectPool == null || potionEffectPool == null) {
                initRandomPool(player);
            }
            if (allEffectPool.isEmpty())
                return;

            NBTTagCompound tag = getOrCreateTag(stack);
            int count = tag.hasKey("AllEffectCount") ? tag.getInteger("AllEffectCount") : 1;
            Random rand = player.getRNG();
            Potion effect;
            Potion otherEffect = getEffect(stack, 1 - index);
            do {
                if (index == 1 || rand.nextInt((count + 1) / 2 + 1) == 0) {
                    effect = allEffectPool.get(rand.nextInt(allEffectPool.size()));
                    tag.setInteger("AllEffectCount", count + 1);
                } else {
                    if (potionEffectPool.isEmpty()) {
                        effect = allEffectPool.get(rand.nextInt(allEffectPool.size()));
                    } else {
                        effect = potionEffectPool.get(rand.nextInt(potionEffectPool.size()));
                    }
                }
            } while (effect == otherEffect);

            Potion oldEffect = getEffect(stack, index);
            if (oldEffect != null && player.isPotionActive(oldEffect)) {
                player.removePotionEffect(oldEffect);
            }
            setEffect(stack, index, effect);
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
