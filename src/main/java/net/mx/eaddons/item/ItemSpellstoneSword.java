package net.mx.eaddons.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.spellstone.SpellstoneAbilities;
import net.mx.eaddons.spellstone.SpellstoneData;
import net.mx.eaddons.spellstone.SpellstoneForm;
import net.mx.eaddons.spellstone.SwordShieldUse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 术质共鸣者：吃入术石获得对应形态，十种形态各有主动、被动与满级效果。
 * 移植自 EL+ 1.21.1 的 {@code SpellstoneSword}（经 1.20.1 的 enigmaticechoes 中转）。
 *
 * <p>交互全部走副手，与 1.20.1 一致：主手本剑 + 副手术石长按右键吸入；副手术核长按右键吐出；
 * 副手其它物品则触发形态主动。原版还支持在物品栏把术核拖叠到剑上升级，
 * 1.12.2 没有 {@code overrideOtherStackedOnMe} 这套 API，改为「潜行右键 + 副手术核」。
 *
 * <p>本类只管注册、外观、状态机与分发；各形态的技能实现在 {@link SpellstoneAbilities}。
 */
public class ItemSpellstoneSword extends net.minecraft.item.ItemSword {
    public static final ItemSpellstoneSword INSTANCE = new ItemSpellstoneSword();

    /** 星云形态的攻击距离修饰符，与虚空充能共用（两者互斥）。 */
    private static final UUID REACH_UUID = UUID.fromString("6b3f6a8e-0e1a-4c2b-9d3e-000000000010");

    public ItemSpellstoneSword() {
        // 必须继承 ItemSword：moremod 的武器升级台会判 output instanceof ItemSword，
        // 更好战斗等模组也按剑来识别。材质只是占位，攻击力/攻速/耐久/附魔/修复全部由下面覆盖。
        super(ToolMaterial.DIAMOND);
        setUnlocalizedName("spellstone_sword");
        setRegistryName("spellstone_sword");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setMaxDamage(SpellstoneSwordConfig.durability);
    }

    // ============================ 外观 ============================

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return SpellstoneData.hasResonance(stack) || SpellstoneData.hasLink(stack)
                ? EnumRarity.EPIC : EnumRarity.RARE;
    }

    /**
     * 共鸣后按术石改名，对应 1.20.1 的 getDescriptionId 变体。
     * <p>改写 unlocalizedName 而不是 getItemStackDisplayName：后者服务端也会调用，
     * 在里面碰 {@code net.minecraft.client.resources.I18n} 会让专用服务器 NoClassDefFound。
     * 交给原版去拼 ".name" 并翻译，两端都安全。
     */
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        SpellstoneForm link = SpellstoneData.getLinkForm(stack);
        if (link != SpellstoneForm.NONE && SpellstoneData.isLinkActive(stack)) {
            if (link != SpellstoneForm.PRIMEVAL_CUBE) {
                return super.getUnlocalizedName() + "." + link.path();
            }
            // 原初共鸣：名字带上当前子形态，贴图则始终是原初
            SpellstoneForm sub = SpellstoneData.getSubForm(stack);
            return sub == SpellstoneForm.PRIMEVAL_CUBE
                    ? super.getUnlocalizedName() + "." + link.path()
                    : super.getUnlocalizedName() + ".primeval." + sub.path();
        }
        SpellstoneForm form = SpellstoneData.getForm(stack);
        return form == SpellstoneForm.NONE
                ? super.getUnlocalizedName(stack)
                : super.getUnlocalizedName() + "." + form.path();
    }

    /**
     * 物品格下沿画能量条而非耐久条：能量是战斗资源、需要实时可见，耐久改在 tooltip 里显示。
     * 1.20.1 用独立的 IItemDecorator 两条并存，1.12.2 只有这一条，只能二选一。
     */
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return SpellstoneData.getMaxEnergy(stack) > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int max = SpellstoneData.getMaxEnergy(stack);
        return max <= 0 ? 0.0D : 1.0D - (double) SpellstoneData.getEnergy(stack) / max;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return SpellstoneData.getForm(stack).color() & 0xFFFFFF;
    }

    // ============================ 动态属性 ============================

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot != EntityEquipmentSlot.MAINHAND) {
            return map;
        }
        SpellstoneForm form = SpellstoneData.getForm(stack);
        double[] bonus = getAttributeBonus(form, SpellstoneData.getLevel(stack));
        map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER, "Weapon modifier", SpellstoneSwordConfig.baseDamage + bonus[0], 0));
        map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(
                ATTACK_SPEED_MODIFIER, "Weapon modifier", -4.0D + bonus[1], 0));
        // 攻击距离：走 Forge 的 generic.reachDistance，写法同 EL 的休眠之眼。
        // ReachFix 本身就是基于这个属性工作的（getEntityReach = getBlockReach + entityReach - reach），
        // 所以这里的加成对方块交互与实体攻击同时生效。setSaved(false) 防止存盘后重复叠加。
        // 星云的攻击距离也在原初共鸣常驻的持握被动之列
        if (form == SpellstoneForm.EYE_OF_NEBULA || SpellstoneData.isPrimevalActive(stack)) {
            map.put(EntityPlayer.REACH_DISTANCE.getName(), new AttributeModifier(
                    REACH_UUID, "Spell reach", SpellstoneSwordConfig.nebulaReachBonus, 0).setSaved(false));
        } else if (form == SpellstoneForm.VOID_PEARL && SpellstoneData.getEnergy(stack) > 0) {
            map.put(EntityPlayer.REACH_DISTANCE.getName(), new AttributeModifier(
                    REACH_UUID, "Spell reach", 0.5D, 0).setSaved(false));
        }
        return map;
    }

    /**
     * 返回 {攻击加成, 攻速值}，忠实移植 EL+ 的 getAttributeBonus。
     * 攻速那一项是最终面板值（修饰符写成 -4.0 + 它，抵消玩家 4.0 的基础攻速）。
     */
    private static double[] getAttributeBonus(SpellstoneForm form, int level) {
        int f = form.index();
        if (f == 5 && level > 4) {
            return new double[]{9.0D, 2.4D};   // 复苏之叶满级独有的一跳
        }
        boolean flag = level > 2;
        switch (f) {
            case 1: return new double[]{flag ? 5.0D : 3.5D, flag ? 1.45D : 1.35D};
            case 2: return new double[]{flag ? 6.0D : 4.0D, flag ? 1.4D : 1.25D};
            case 3: return new double[]{flag ? 4.0D : 2.5D, flag ? 1.5D : 1.35D};
            case 4: return new double[]{flag ? 3.0D : 2.0D, flag ? 1.75D : 1.6D};
            case 5: return new double[]{flag ? 2.5D : 1.5D, flag ? 2.0D : 1.8D};
            case 6: return new double[]{flag ? 2.0D : 1.0D, 2.0D};
            case 7: return new double[]{flag ? 3.5D : 2.5D, flag ? 1.7D : 1.65D};
            case 8: return new double[]{flag ? 2.0D : 1.0D, 1.6D};
            case 9: return new double[]{flag ? 4.0D : 3.0D, flag ? 1.8D : 1.75D};
            case 10: return new double[]{flag ? 5.5D : 3.0D, flag ? 1.5D : 1.4D};
            case 11: return new double[]{5.0D, 1.6D};    // 纯原初（链接即满级，不分档）
            case 12: return new double[]{7.0D, 1.6D};    // 非欧
            default: return new double[]{flag ? 2.0D : 0.0D, flag ? 1.6D : 1.5D};
        }
    }

    // ============================ 右键：共鸣建立 / 解除 / 形态主动 ============================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        ItemStack offhand = player.getHeldItemOffhand();
        boolean resonating = SpellstoneData.hasResonance(stack);
        boolean linked = SpellstoneData.hasLink(stack);
        boolean offhandCore = offhand.getItem() == ItemSpellcore.INSTANCE;
        SpellstoneForm offhandLink = SpellstoneForm.linkFormOf(offhand);

        // 两颗立方：连按两次右键建立链接，术石留在副手不被吃入；同种术石再连按两次解除
        if (linked && offhandLink == SpellstoneData.getLinkForm(stack)) {
            return confirmLink(world, player, stack, offhandLink, true);
        }
        if (!resonating && !linked && offhandLink != SpellstoneForm.NONE) {
            return confirmLink(world, player, stack, offhandLink, false);
        }

        // 潜行 + 副手术核：消耗一枚提升共鸣等级（替代 1.20.1 的物品栏拖叠）
        if (resonating && offhandCore && player.isSneaking()) {
            int level = SpellstoneData.getLevel(stack);
            if (level < SpellstoneData.MAX_LEVEL) {
                SpellstoneData.setLevel(stack, level + 1);
                if (!player.capabilities.isCreativeMode) {
                    offhand.shrink(1);
                }
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                        SoundCategory.PLAYERS, 1.0F, 1.4F + player.getRNG().nextFloat() * 0.2F);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        // 解除共鸣：已共鸣 + 副手术核 → 窗口内再右键一次吐出术石
        if (resonating && offhandCore) {
            return confirmOrSwap(world, player, stack, true);
        }
        // 建立共鸣：未共鸣 + 副手为可共鸣术石 → 同样二次确认
        if (!resonating && !linked && SpellstoneForm.isResonatable(offhand)) {
            return confirmOrSwap(world, player, stack, false);
        }
        // 剑盾：普通右键举盾，按住共鸣技能键才放形态主动
        if (SwordShieldUse.wantsBlock(player, stack)) {
            return SwordShieldUse.raise(player, hand, stack);
        }
        // 已共鸣（含链接生效中）+ 副手非术核 → 形态主动
        if (resonating || SpellstoneData.getForm(stack) != SpellstoneForm.NONE) {
            return SpellstoneAbilities.useForm(world, player, hand, stack, SpellstoneData.getForm(stack));
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    // ============================ 蓄力 ============================

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        if (SwordShieldUse.isShieldUse(stack)) {
            return 72000;   // 与 chocotweak 剑盾一致
        }
        switch (SpellstoneData.getForm(stack)) {
            case ANGEL_BLESSING: return 36;
            case ILLUSION_LANTERN: return 80;
            case VOID_PEARL: return 24;
            case NONE: return SpellstoneSwordConfig.resonateTime;
            default: return 32000;   // 长按型，由 onPlayerStoppedUsing 结束
        }
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        if (SwordShieldUse.isShieldUse(stack)) {
            return EnumAction.BLOCK;
        }
        switch (SpellstoneData.getForm(stack)) {
            case BLAZING_CORE:
            case VOID_PEARL:
            case LOST_ENGINE:
                return EnumAction.BLOCK;
            default:
                return EnumAction.BOW;
        }
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase user, int count) {
        if (!(user instanceof EntityPlayer) || SwordShieldUse.isShieldUse(stack)) {
            return;
        }
        SpellstoneAbilities.onChargeTick((EntityPlayer) user, stack, getMaxItemUseDuration(stack) - count);
    }

    /**
     * 使用中服务端改了剑的 NBT（能量、冰盾、耐久）会让客户端换一个物品实例，原版默认按实例判断就此打断使用，
     * 举盾或蓄力会在客户端无故落下。同一把剑、同一形态就继续，并把技能标记带到新实例上。
     */
    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        boolean same = newStack.getItem() == this
                && SpellstoneData.getForm(oldStack) == SpellstoneData.getForm(newStack)
                && SwordShieldUse.hasShield(oldStack) == SwordShieldUse.hasShield(newStack);
        if (same) {
            SwordShieldUse.transfer(oldStack, newStack);
        }
        return same;
    }

    // ============================ 共鸣的建立与解除 ============================

    /**
     * 第一次右键开确认窗口，窗口内第二次右键才真正换石头。
     *
     * @param eject true 吐出、false 吸入
     */
    private ActionResult<ItemStack> confirmOrSwap(World world, EntityPlayer player, ItemStack stack, boolean eject) {
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (SpellstoneData.getConfirm(stack) > 0) {
            SpellstoneData.setConfirm(stack, 0);
            if (eject) {
                ejectResonance(stack, player);
            } else {
                absorbResonance(stack, player);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        SpellstoneData.setConfirm(stack, SpellstoneSwordConfig.resonateTime);
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation(
                eject ? "message.eaddons.resonance.confirm_eject" : "message.eaddons.resonance.confirm_bind"), true);
        world.playSound(null, player.getPosition(), SoundEvents.BLOCK_NOTE_HAT,
                SoundCategory.PLAYERS, 0.7F, 1.6F);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 两颗立方的链接同样走「窗口内连按两次」，但不搬运任何物品：
     * 建立时术石留在副手，解除时也不吐东西，所以不存在解除刷石的路径。
     */
    private ActionResult<ItemStack> confirmLink(World world, EntityPlayer player, ItemStack stack,
                                                SpellstoneForm form, boolean release) {
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (SpellstoneData.getConfirm(stack) > 0) {
            SpellstoneData.setConfirm(stack, 0);
            if (release) {
                net.mx.eaddons.spellstone.ResonanceLink.unlink(player, stack);
            } else {
                net.mx.eaddons.spellstone.ResonanceLink.link(player, stack, form);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        SpellstoneData.setConfirm(stack, SpellstoneSwordConfig.resonateTime);
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation(
                release ? "message.eaddons.resonance.confirm_unlink" : "message.eaddons.resonance.confirm_link"), true);
        world.playSound(null, player.getPosition(), SoundEvents.BLOCK_NOTE_HAT,
                SoundCategory.PLAYERS, 0.7F, 1.6F);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 吐出共鸣的术石，换走一枚术核。术核可堆叠到 16，成叠时不能整格替换副手，
     * 只扣一枚，术石进背包、放不下的丢出。
     */
    private void ejectResonance(ItemStack stack, EntityPlayer player) {
        if (!SpellstoneData.hasResonance(stack)) {
            return;
        }
        ItemStack resonance = SpellstoneData.getResonance(stack).copy();
        ItemStack offhand = player.getHeldItemOffhand();
        if (offhand.getCount() > 1) {
            offhand.shrink(1);
            // 走 inventory 版：EntityPlayer 版会多播一声盔甲穿戴音效；可能只放进一部分，剩余留在 resonance 里
            player.inventory.addItemStackToInventory(resonance);
            if (!resonance.isEmpty()) {
                player.dropItem(resonance, false);
            }
        } else {
            player.setHeldItem(EnumHand.OFF_HAND, resonance);
        }
        SpellstoneData.clearResonance(stack);
        SpellstoneData.setEnergy(stack, 0);
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.8F);
    }

    /** 吃入副手的术石，副手换成术核。术石均不可堆叠（上限 1），整格替换不会吞物品。 */
    private void absorbResonance(ItemStack stack, EntityPlayer player) {
        ItemStack offhand = player.getHeldItemOffhand();
        if (SpellstoneData.hasResonance(stack) || !SpellstoneForm.isResonatable(offhand)) {
            return;
        }
        SpellstoneData.setResonance(stack, offhand.copy());
        player.setHeldItem(EnumHand.OFF_HAND, new ItemStack(ItemSpellcore.INSTANCE));
        SpellstoneData.setEnergy(stack, 0);
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int timeLeft) {
        if (user instanceof EntityPlayer && !SwordShieldUse.isShieldUse(stack)) {
            SpellstoneAbilities.onChargeRelease(world, (EntityPlayer) user, stack,
                    getMaxItemUseDuration(stack) - timeLeft);
        }
        SwordShieldUse.endUse(stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        SwordShieldUse.endUse(stack);
        if (!(user instanceof EntityPlayer)) {
            return stack;
        }
        EntityPlayer player = (EntityPlayer) user;
        // 天使/幻影蓄力完成：补满能量
        SpellstoneAbilities.onChargeFinish(world, player, stack);
        return stack;
    }

    // ============================ 右键方块 ============================

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (SwordShieldUse.wantsBlock(player, stack)) {
            return EnumActionResult.PASS;   // 让原版接着走 onItemRightClick 举盾
        }
        return SpellstoneAbilities.useOnBlock(world, player, stack, pos, hitX, hitY, hitZ);
    }

    // ============================ 战斗 ============================

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        SpellstoneAbilities.onHitEntity(stack, target, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0.0F) {
            stack.damageItem(2, entity);
        }
        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            SpellstoneAbilities.inventoryTick(world, (EntityPlayer) entity, stack, selected);
        }
    }

    @Override
    public int getItemEnchantability() {
        return 64;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == ItemSpellstoneDebris.INSTANCE;
    }

    /** 与 1.20.1 的 canAttackBlock 对应：创造模式左键不破坏方块，手感同剑。 */
    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        net.mx.eaddons.client.SpellstoneSwordTooltip.addInformation(stack, list);
    }

    /** 供各形态统一取「佩戴者攻击力」。 */
    public static float getAttackDamage(EntityLivingBase user) {
        return (float) user.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
    }

    /** 1.12.2 没有 isInWaterRainOrBubble，用 isWet 近似（在水中或正在淋雨）。 */
    public static boolean isWet(EntityLivingBase user) {
        return user.isWet();
    }

    public static int floor(double value) {
        return MathHelper.floor(value);
    }
}
