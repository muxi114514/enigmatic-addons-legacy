package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 魔法石英权杖：移植自 1.20 神遗拓展 QuartzScepter。
 * 右键蓄力持续发射魔法石英匕首；佩戴魔法石英戒指提速提力、持有魔法石英花减少冷却；
 * 幸运值提高匕首伤害。所有数值见 {@link QuartzScepterConfig}。
 */
public class ItemQuartzScepter extends Item {
    public static final ItemQuartzScepter INSTANCE = new ItemQuartzScepter();

    /** 最大蓄力时长（tick），同原版固定 100。 */
    private static final int MAX_USE_DURATION = 100;
    /** 每 8 tick 蓄力消耗 1 点耐久。 */
    private static final int DURABILITY_PER_TICKS = 8;

    private final Random random = new Random();

    public ItemQuartzScepter() {
        // 构造时设一个正的基线耐久，保证 isDamageable()=true；真实耐久由 getMaxDamage 覆写读配置，
        // 以规避"物品注册早于配置加载(preInit)"的时序问题（同 ItemTotemOfMalice 做法）。
        setMaxDamage(160);
        maxStackSize = 1;
        setUnlocalizedName("quartz_scepter");
        setRegistryName("quartz_scepter");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return QuartzScepterConfig.durability;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public int getItemEnchantability() {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == Items.QUARTZ || super.getIsRepairable(toRepair, repair);
    }

    // 近战攻击时额外掉 2 点耐久，同原版 hurtEnemy
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(2, attacker);
        return true;
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(slot);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier",
                            QuartzScepterConfig.attackDamage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier",
                            QuartzScepterConfig.attackSpeed, 0));
        }
        return multimap;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    /** 蓄力期间每 tick 调用：按有效间隔发射匕首。count 为剩余 tick（从 100 递减）。 */
    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        boolean hasRing = hasQuartzRing(player);
        int interval = QuartzScepterConfig.getEffectiveInterval(hasRing);
        if (count % interval != 0) return;

        World world = player.world;
        EntityQuartzDagger dagger = new EntityQuartzDagger(world, player);

        // 在玩家眼前一小范围内随机生成，再沿视线方向射出
        double x = player.posX + (random.nextFloat() - 0.5F) * 0.6F;
        double y = player.posY + player.getEyeHeight() - 0.4 + random.nextFloat() * 0.6F;
        double z = player.posZ + (random.nextFloat() - 0.5F) * 0.6F;
        dagger.setPosition(x, y, z);

        float velocity = (float) (hasRing
                ? QuartzScepterConfig.daggerVelocityWithRing
                : QuartzScepterConfig.daggerVelocity);
        dagger.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, velocity, 1.0F);

        dagger.setDamage(QuartzScepterConfig.daggerBaseDamage + getLuckBonus(player));
        dagger.setNoGravity(true);
        world.spawnEntity(dagger);

        // 1.12.2 无三叉戟音效，用箭矢发射声替代原版 TRIDENT_THROW
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.2F, 1.6F);
    }

    /** 蓄满自动结束：结算耐久与冷却。 */
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            int cooldown = QuartzScepterConfig.baseCooldown;
            if (!hasQuartzFlower(player)) {
                cooldown += QuartzScepterConfig.noFlowerCooldownPenalty;
            }
            stack.damageItem(getMaxItemUseDuration(stack) / DURABILITY_PER_TICKS, player);
            player.getCooldownTracker().setCooldown(this, cooldown);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.swingArm(player.getActiveHand());
        }
        return stack;
    }

    /** 提前松手：按已蓄力时长结算耐久与冷却。 */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        int elapsed = getMaxItemUseDuration(stack) - timeLeft;
        stack.damageItem(elapsed / DURABILITY_PER_TICKS, player);
        if (!hasQuartzFlower(player)) {
            elapsed += QuartzScepterConfig.releaseNoFlowerPenalty;
        }
        player.getCooldownTracker().setCooldown(this, 20 + elapsed / 5);
        player.swingArm(player.getActiveHand());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.quartz_scepter.desc1"));
            list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.quartz_scepter.desc2"));
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.quartz_scepter.desc3"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.quartz_scepter.brief"));
            list.add(I18n.format("tooltip.eaddons.quartz_scepter.hold_shift"));
        }
        list.add("");
    }

    /** 幸运值转化的匕首额外伤害。 */
    private static int getLuckBonus(EntityPlayer player) {
        IAttributeInstance luck = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
        if (luck == null) return 0;
        double value = luck.getAttributeValue();
        return Math.max(MathHelper.floor(value * QuartzScepterConfig.luckDamageFactor + 0.5), 0);
    }

    /** 是否佩戴魔法石英戒指（饰品栏）。 */
    private static boolean hasQuartzRing(EntityPlayer player) {
        return BaublesApi.isBaubleEquipped(player, ItemQuartzRing.INSTANCE) != -1;
    }

    /** 背包/副手中是否持有魔法石英花，对应原版 hasItem 判定。 */
    private static boolean hasQuartzFlower(EntityPlayer player) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemArtificialFlower) return true;
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemArtificialFlower) return true;
        }
        return false;
    }
}
