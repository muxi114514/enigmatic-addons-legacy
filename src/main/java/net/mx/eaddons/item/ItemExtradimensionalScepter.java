package net.mx.eaddons.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 超维权杖：移植自 1.20 神遗拓展 ExtradimensionalScepter（完整照搬）。
 * 双模式：
 *  - 运输模式（默认）：右键收纳较弱生物进权杖，右键地面放出；
 *  - 战斗模式：右键蓄力发射射线累积「超维值」，够高时由 {@link ExtradimensionalScepterEventHandler}
 *    触发大招，将周围敌人封印进 {@link EntityExtradimensionalLock}。
 * 潜行右键切换模式。数值见 {@link ExtradimensionalScepterConfig}。
 */
public class ItemExtradimensionalScepter extends Item {
    public static final ItemExtradimensionalScepter INSTANCE = new ItemExtradimensionalScepter();

    private static final int MAX_USE_DURATION = 100;
    private static final int DURABILITY_PER_TICKS = 8;

    public ItemExtradimensionalScepter() {
        setMaxDamage(240); // 基线，真实耐久由 getMaxDamage 读配置（注册早于配置加载）
        maxStackSize = 1;
        setUnlocalizedName("extradimensional_scepter");
        setRegistryName("extradimensional_scepter");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ExtradimensionalScepterConfig.durability;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public int getItemEnchantability() {
        return 24;
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
        Item eye = getEye();
        return (eye != null && repair.getItem() == eye) || super.getIsRepairable(toRepair, repair);
    }

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
                            ExtradimensionalScepterConfig.attackDamage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier",
                            ExtradimensionalScepterConfig.attackSpeed, 0));
        }
        return multimap;
    }

    // ============================ 右键：切换模式 / 进入战斗蓄力 ============================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (Helper.validScepter(player, stack)) {
            if (player.isSneaking()) {
                Helper.switchMode(stack, player);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            if (Helper.isCombatMode(stack)) {
                player.setActiveHand(hand);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    // ============================ 运输模式：右键生物收纳 ============================

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (Helper.isCombatMode(stack) || player.isSneaking()) return false;
        if (!Helper.validTarget(player, target, 2.5) || !Helper.validScepter(player, stack)) return false;

        World world = player.world;
        if (!world.isRemote) {
            if (Helper.isValid(stack)) {
                // 已存有生物，此时右键生物不做操作（同原版被注释掉的骑乘逻辑）
                return true;
            }
            Helper.storeEntity(stack, target);
            target.setDead();
            world.playSound(null, player.getPosition(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            stack.damageItem(2, player);
            player.getCooldownTracker().setCooldown(this, Helper.getTransCooldown(player));
            spawnBanishParticles(world, target.posX, target.posY, target.posZ, target.width, target.height);
        }
        return true;
    }

    // ============================ 运输模式：右键地面放出 ============================

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (Helper.isCombatMode(stack) || facing != EnumFacing.UP || !Helper.validScepter(player, stack)) {
            return EnumActionResult.PASS;
        }
        if (!Helper.isValid(stack) || player.isSneaking()) return EnumActionResult.PASS;

        ResourceLocation type = Helper.getType(stack);
        if (type == null) return EnumActionResult.FAIL;

        if (!world.isRemote) {
            player.getCooldownTracker().setCooldown(this, Helper.getTransCooldown(player));
            stack.damageItem(2, player);
            Entity entity = EntityList.createEntityFromNBT(Helper.getInfo(stack), world);
            if (entity == null) {
                Helper.setValid(stack, false);
                return EnumActionResult.FAIL;
            }
            double spawnX = pos.getX() + hitX;
            double spawnY = pos.getY() + hitY;
            double spawnZ = pos.getZ() + hitZ;
            entity.setLocationAndAngles(spawnX, spawnY, spawnZ, entity.rotationYaw, entity.rotationPitch);
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
            world.spawnEntity(entity);
            Helper.setValid(stack, false);
            world.playSound(null, player.getPosition(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            spawnBanishParticles(world, entity.posX, entity.posY, entity.posZ, entity.width, entity.height);
        }
        return EnumActionResult.SUCCESS;
    }

    // ============================ 战斗模式：蓄力累积超维值 ============================

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase user, int count) {
        if (!(user instanceof EntityPlayer)) return;
        World world = user.world;

        Vec3d view = user.getLookVec().scale(0.4);
        double x = user.posX;
        double z = user.posZ;
        double y = user.posY + user.getEyeHeight() - 0.25F;
        AxisAlignedBB box = new AxisAlignedBB(x - 0.2, y - 0.2, z - 0.2, x + 0.2, y + 0.2, z + 0.2);
        float rand = user.getRNG().nextFloat();
        x += view.x * rand;
        z += view.z * rand;
        y += view.y * rand;

        List<EntityLivingBase> targets = new ArrayList<>();
        for (int i = 1; i < 56; i++) {
            targets = findTargets(world, box, user, 5.0F);
            if (!targets.isEmpty()) break;
            box = box.offset(view);
            x = ((view.x + x) * 2 + (user.getRNG().nextFloat() - 0.5F) * 0.5F) / 2;
            z = ((view.z + z) * 2 + (user.getRNG().nextFloat() - 0.5F) * 0.5F) / 2;
            y = ((view.y + y) * 2 + (user.getRNG().nextFloat() - 0.5F) * 0.5F) / 2;
            if (user.ticksExisted % 4 == 0) {
                if (world.isRemote) {
                    world.spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH, x, y, z, 0, -0.5, 0);
                } else {
                    world.playSound(null, new BlockPos(x, y, z), SoundEvents.BLOCK_PORTAL_AMBIENT,
                            SoundCategory.PLAYERS, 0.4F, 1.2F + 0.2F * user.getRNG().nextFloat());
                }
            }
            // 区块守卫：射线可能跨越到未加载区块，禁止直接 getBlockState 触发同步加载
            BlockPos bp = new BlockPos(x, y, z);
            if (!world.isBlockLoaded(bp)) break;
            if (world.getBlockState(bp).isOpaqueCube()) break;
        }

        if (targets.isEmpty() || world.isRemote) return;

        double userDamage = user.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        for (EntityLivingBase target : targets) {
            NBTTagCompound data = target.getEntityData();
            int counter = data.getInteger("ExtradimensionCounter");
            int add = 2 * MathHelper.floor(Math.sqrt(Math.max((user.getHealth() + userDamage) * 5, 0)));
            data.setInteger("ExtradimensionCounter", counter + add);
            int healthCounter = Math.max(MathHelper.floor(target.getHealth() * 20), 200);
            if (counter > healthCounter / 2) {
                target.attackEntityFrom(DamageSource.OUT_OF_WORLD, (float) (userDamage / 4.0));
            }
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            stack.damageItem(getMaxItemUseDuration(stack) / DURABILITY_PER_TICKS, player);
            player.getCooldownTracker().setCooldown(this, Helper.getOverheatCooldown(player));
            world.playSound(null, player.getPosition(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.swingArm(player.getActiveHand());
        }
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        int elapsed = getMaxItemUseDuration(stack) - timeLeft;
        stack.damageItem(elapsed / DURABILITY_PER_TICKS, player);
        player.getCooldownTracker().setCooldown(this, Helper.getOverheatCooldown(player) / 5 + elapsed / 5);
        player.swingArm(player.getActiveHand());
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            boolean combat = Helper.isCombatMode(stack);
            if (!combat) {
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.extradimensional_scepter.desc1"));
                list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.extradimensional_scepter.cooldown",
                        ExtradimensionalScepterConfig.transportingCooldown / 20));
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.extradimensional_scepter.mode_transport"));
            } else {
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.extradimensional_scepter.desc2"));
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.extradimensional_scepter.desc3"));
                list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.extradimensional_scepter.cooldown",
                        ExtradimensionalScepterConfig.overheatingCooldown / 20));
                list.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.eaddons.extradimensional_scepter.mode_combat"));
            }
            if (!combat && Helper.isValid(stack)) {
                ResourceLocation type = Helper.getType(stack);
                if (type != null) {
                    String name = I18n.format("entity." + EntityList.getTranslationName(type) + ".name");
                    list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.extradimensional_scepter.info", name));
                }
            }
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.extradimensional_scepter.brief"));
            list.add(I18n.format("tooltip.eaddons.extradimensional_scepter.hold_shift"));
        }
        list.add("");
    }

    // ============================ 共享工具 ============================

    /** 封印/放出时的紫色爆散粒子，替代 1.20 的 PacketExtradimensionParticles（服务端 spawnParticle 会广播）。 */
    public static void spawnBanishParticles(World world, double x, double y, double z, double width, double height) {
        int count = (int) (24 + height * 2 + width * 5);
        for (int i = 0; i < count; i++) {
            double xo = width * (0.5F - world.rand.nextFloat());
            double yo = height * world.rand.nextFloat();
            double zo = width * (0.5F - world.rand.nextFloat());
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH, x + xo, y + yo, z + zo, 0, 0, 0);
            if (i % 2 == 0) {
                world.spawnParticle(net.minecraft.util.EnumParticleTypes.CLOUD, x + xo, y + yo, z + zo, 0, 0, 0);
            }
            if (i % 8 == 0) {
                world.spawnParticle(net.minecraft.util.EnumParticleTypes.EXPLOSION_NORMAL, x + xo * 2, y + yo, z + zo * 2, 0, 0, 0);
            }
        }
    }

    private static List<EntityLivingBase> findTargets(World world, AxisAlignedBB box, EntityLivingBase user, float threshold) {
        List<EntityLivingBase> result = new ArrayList<>();
        for (EntityLivingBase living : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (living != user && Helper.validTarget(user, living, threshold)) {
                result.add(living);
            }
        }
        return result;
    }

    static Item getEye() {
        return net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("enigmaticlegacy", "extradimensional_eye"));
    }

    // ============================ NBT 与判定辅助（对应 1.20 内部 Helper） ============================

    public static class Helper {
        public static int getTransCooldown(EntityPlayer player) {
            return player.capabilities.isCreativeMode ? 5 : ExtradimensionalScepterConfig.transportingCooldown;
        }

        public static int getOverheatCooldown(EntityPlayer player) {
            return player.capabilities.isCreativeMode ? 20 : ExtradimensionalScepterConfig.overheatingCooldown;
        }

        public static boolean validScepter(EntityPlayer player, ItemStack stack) {
            return !player.getCooldownTracker().hasCooldown(INSTANCE)
                    && stack.getItemDamage() < stack.getMaxDamage();
        }

        /** 目标非玩家，且（施术者创造模式 或 施术者血量×阈值 > 目标血量）。 */
        public static boolean validTarget(EntityLivingBase from, EntityLivingBase to, double threshold) {
            boolean creative = from instanceof EntityPlayer && ((EntityPlayer) from).capabilities.isCreativeMode;
            return !(to instanceof EntityPlayer) && (creative || from.getHealth() * threshold > to.getHealth());
        }

        public static boolean isCombatMode(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null && tag.getBoolean("CombatMode");
        }

        public static void switchMode(ItemStack stack, EntityPlayer player) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            boolean combat = !tag.getBoolean("CombatMode");
            tag.setBoolean("CombatMode", combat);
            player.world.playSound(null, player.getPosition(),
                    SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.8F, combat ? 1.5F : 0.7F);
        }

        @Nullable
        public static ResourceLocation getType(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && tag.hasKey("ExtradimensionalType")) {
                ResourceLocation key = new ResourceLocation(tag.getString("ExtradimensionalType"));
                if (EntityList.getClass(key) == null) {
                    setValid(stack, false);
                    return null;
                }
                return key;
            }
            return null;
        }

        public static NBTTagCompound getInfo(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag == null ? new NBTTagCompound() : tag.getCompoundTag("ExtradimensionalEntity");
        }

        public static boolean isValid(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null && tag.getBoolean("ExtradimensionalValid");
        }

        public static void setValid(ItemStack stack, boolean valid) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setBoolean("ExtradimensionalValid", valid);
            if (!valid) tag.removeTag("ExtradimensionalEntity");
        }

        /** 把生物完整 NBT 存进权杖：写入 NBT、去掉 UUID 防重、记录类型键、置为有效。 */
        public static void storeEntity(ItemStack stack, EntityLivingBase entity) {
            NBTTagCompound entityTag = new NBTTagCompound();
            entity.writeToNBTOptional(entityTag);
            entityTag.removeTag("UUIDMost");
            entityTag.removeTag("UUIDLeast");

            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setTag("ExtradimensionalEntity", entityTag);
            ResourceLocation key = EntityList.getKey(entity);
            if (key != null) tag.setString("ExtradimensionalType", key.toString());
            setValid(stack, true);
        }
    }
}
