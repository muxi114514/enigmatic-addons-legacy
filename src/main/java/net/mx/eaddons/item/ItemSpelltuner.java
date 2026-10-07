package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.spellstone.SpellstoneForm;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 术质调谐器：记录一颗术石，佩戴后获得该术石的调谐被动（有的是本体被动的弱化版，有的是另设的能力），另加幸运 +1。
 * 移植自 EL+ 的 {@code Spelltuner}，与术质共鸣者没有耦合。
 *
 * <p>记录方式改了：1.20.1 是在物品栏把调谐器拖叠到术石上（依赖 1.17+ 的 {@code overrideStackedOnOther}），
 * 1.12.2 没有这套 API，改为**主手调谐器 + 副手术石右键**，和共鸣者的副手交互保持一致。
 *
 * <p>与直接佩戴术石本体不叠加——{@link #hasTune} 里会检查饰品栏是否已戴着同一颗术石。
 */
public class ItemSpelltuner extends Item implements IBauble {
    public static final ItemSpelltuner INSTANCE = new ItemSpelltuner();

    private static final String TUNED = "TunedSpellstone";
    private static final UUID LUCK_UUID = UUID.fromString("6b3f6a8e-0e1a-4c2b-9d3e-000000000061");
    private static final UUID GOLEM_KB_UUID = UUID.fromString("6b3f6a8e-0e1a-4c2b-9d3e-000000000060");

    public ItemSpelltuner() {
        setUnlocalizedName("spelltuner");
        setRegistryName("spelltuner");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getTuned(stack) != null;
    }

    // ============================ 记录的术石 ============================

    public static void setTuned(ItemStack tuner, Item spellstone) {
        ResourceLocation id = spellstone.getRegistryName();
        if (id == null) {
            return;
        }
        NBTTagCompound tag = tuner.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            tuner.setTagCompound(tag);
        }
        tag.setString(TUNED, id.toString());
    }

    @Nullable
    public static Item getTuned(ItemStack tuner) {
        NBTTagCompound tag = tuner.getTagCompound();
        if (tag == null || !tag.hasKey(TUNED)) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString(TUNED)));
    }

    /**
     * 该实体是否通过调谐器获得了指定形态的弱化被动。
     * <p>佩戴调谐器、其记录的正是该形态的术石、且**没有**同时佩戴该术石本体（避免双倍）。
     */
    public static boolean hasTune(EntityLivingBase entity, SpellstoneForm form) {
        if (!(entity instanceof EntityPlayer) || form.itemId() == null) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity;
        // 先查调谐器本身：没戴就直接退，避免后面几趟饰品栏遍历
        int slot = BaublesApi.isBaubleEquipped(player, INSTANCE);
        if (slot == -1) {
            return false;
        }
        Item spellstone = ForgeRegistries.ITEMS.getValue(form.itemId());
        if (spellstone == null || BaublesApi.isBaubleEquipped(player, spellstone) != -1) {
            return false;   // 已戴术石本体，调谐不再叠加
        }
        ItemStack tuner = BaublesApi.getBaublesHandler(player).getStackInSlot(slot);
        return !tuner.isEmpty() && getTuned(tuner) == spellstone;
    }

    /** 快速判定：伤害事件里先用它整体早退，省掉逐形态的饰品栏遍历。 */
    public static boolean isWearing(EntityLivingBase entity) {
        return entity instanceof EntityPlayer
                && BaublesApi.isBaubleEquipped((EntityPlayer) entity, INSTANCE) != -1;
    }

    // ============================ 记录：主手调谐器 + 副手术石右键 ============================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        ItemStack offhand = player.getHeldItemOffhand();
        if (!SpellstoneForm.isResonatable(offhand)) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (!world.isRemote) {
            setTuned(stack, offhand.getItem());
        }
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F,
                1.3F + player.getRNG().nextFloat() * 0.4F);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // ============================ 佩戴 ============================

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack stack, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            applyModifier(entity, SharedMonsterAttributes.LUCK, LUCK_UUID, "Spelltuner luck", 1.0D, 0);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack stack, EntityLivingBase entity) {
        removeModifier(entity, SharedMonsterAttributes.LUCK, LUCK_UUID);
        removeModifier(entity, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, GOLEM_KB_UUID);
    }

    /**
     * 佩戴期间的被动。幸运每 tick 校一次：Baubles 在某些时机（登录、跨维度）不会走 onEquipped，
     * 属性修饰符又不随饰品存盘，只能在这里兜底补回。
     */
    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        applyModifier(player, SharedMonsterAttributes.LUCK, LUCK_UUID, "Spelltuner luck", 1.0D, 0);

        SpellstoneForm form = activeForm(player, stack);
        // 石魂：抗击退拉满到 1.0（原版 knockBack 在抗性 ≥1 时必不击退），击退事件另在事件类里取消
        if (form == SpellstoneForm.GOLEM_HEART) {
            applyModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE,
                    GOLEM_KB_UUID, "Spelltuner golem", 1.0D, 0);
        } else {
            removeModifier(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, GOLEM_KB_UUID);
        }
        switch (form) {
            case BLAZING_CORE:
                // 炽焰：持续熄火（火焰伤害免疫在事件类里拦）
                if (player.isBurning()) {
                    player.extinguish();
                }
                break;
            case REVIVAL_LEAF:
                // 复苏：每秒按最大生命的百分比回血，有保底
                if (player.ticksExisted % 20 == 0 && player.getHealth() < player.getMaxHealth()) {
                    player.heal((float) Math.max(SpelltunerConfig.revivalRegenMin,
                            player.getMaxHealth() * SpelltunerConfig.revivalRegenPercent / 100.0D));
                }
                break;
            case OCEAN_STONE: {
                // 海洋：清理层，处理戴上之前就染上的寄生虫（阻断层在事件类）
                Potion parasites = SpelltunerEventHandler.parasites();
                if (parasites != null && player.isPotionActive(parasites)) {
                    player.removePotionEffect(parasites);
                }
                // 水下呼吸：氧气一直补满（客户端的气泡条由原版同步）
                if (SpelltunerConfig.oceanWaterBreathing && player.getAir() < 300) {
                    player.setAir(300);
                }
                break;
            }
            case ILLUSION_LANTERN:
                // 冥灯：每秒放下一次还盯着佩戴者、又没被挑衅的亡灵
                if (player.ticksExisted % 20 == 0) {
                    SpelltunerUndeadTruce.scan(player);
                }
                break;
            default:
                break;
        }
    }

    /** 该调谐器当前生效的形态；未记录、或饰品栏里戴着同一颗术石本体（不叠加）时为 NONE。 */
    private static SpellstoneForm activeForm(EntityPlayer player, ItemStack tuner) {
        Item spellstone = getTuned(tuner);
        if (spellstone == null || BaublesApi.isBaubleEquipped(player, spellstone) != -1) {
            return SpellstoneForm.NONE;
        }
        return SpellstoneForm.byItem(spellstone);
    }

    private static void applyModifier(EntityLivingBase entity, IAttribute attribute, UUID uuid,
                                      String name, double amount, int operation) {
        IAttributeInstance inst = entity.getEntityAttribute(attribute);
        if (inst != null && inst.getModifier(uuid) == null) {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, operation).setSaved(false));
        }
    }

    private static void removeModifier(EntityLivingBase entity, IAttribute attribute, UUID uuid) {
        IAttributeInstance inst = entity.getEntityAttribute(attribute);
        if (inst != null && inst.getModifier(uuid) != null) {
            inst.removeModifier(uuid);
        }
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add("");
        Item tuned = getTuned(stack);
        if (tuned == null) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.spelltuner.absent"));
        } else {
            ResourceLocation id = tuned.getRegistryName();
            String name = id == null ? "" : I18n.format("item." + id.getResourcePath() + ".name");
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.spelltuner.context",
                    TextFormatting.GOLD + name));
            if (GuiScreen.isShiftKeyDown()) {
                SpellstoneForm form = SpellstoneForm.byItem(tuned);
                String key = "tooltip.eaddons.spelltuner.passive." + form.path();
                String passive = I18n.format(key, passiveArgs(form));
                list.add(TextFormatting.DARK_PURPLE
                        + (key.equals(passive) ? I18n.format("tooltip.eaddons.spelltuner.passive.none") : passive));
            } else {
                list.add(I18n.format("tooltip.eaddons.spelltuner.hold_shift"));
            }
        }
        list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.spelltuner.hint"));
        list.add("");
    }

    /** tooltip 里带数值的几条，数值取自配置。 */
    @SideOnly(Side.CLIENT)
    private static Object[] passiveArgs(SpellstoneForm form) {
        switch (form) {
            case VOID_PEARL:
                return new Object[]{SpelltunerConfig.voidWardChance, seconds(SpelltunerConfig.voidWardInvulnTicks)};
            case LOST_ENGINE:
                return new Object[]{SpelltunerConfig.engineUseTimeReduction};
            case ILLUSION_LANTERN:
                return new Object[]{SpelltunerConfig.lanternProvokeSeconds, SpelltunerConfig.lanternUndeadReduction};
            case GOLEM_HEART:
                return new Object[]{SpelltunerConfig.golemMeleeReduction};
            case BLAZING_CORE:
                return new Object[]{SpelltunerConfig.blazeIgniteSeconds};
            case EYE_OF_NEBULA:
                return new Object[]{SpelltunerConfig.nebulaMagicReduction};
            case REVIVAL_LEAF:
                return new Object[]{trim(SpelltunerConfig.revivalRegenPercent)};
            case FORGOTTEN_ICE:
                return new Object[]{seconds(SpelltunerConfig.frostFreezeTicks)};
            case ANGEL_BLESSING:
                return new Object[]{SpelltunerConfig.angelAirBonus};
            case OCEAN_STONE:
                return new Object[]{SpelltunerConfig.oceanWetReduction};
            default:
                return new Object[0];
        }
    }

    /** tick 转秒，整数秒不带小数。 */
    @SideOnly(Side.CLIENT)
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.valueOf(ticks / 20.0F);
    }

    /** 整数不带小数点。 */
    @SideOnly(Side.CLIENT)
    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
