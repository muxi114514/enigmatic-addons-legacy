package net.mx.eaddons.item;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import keletu.enigmaticlegacy.item.EAddonsSpellstoneBauble;
import keletu.enigmaticlegacy.util.helper.ExperienceHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 复苏之叶：移植自 1.20 神遗拓展 RevivalLeaf（完整移植，A）。
 * 一枚咒石饰品：凋零免疫 + 火焰/弹射物抗性；佩戴时清除自身负面、加速附近作物、自然回血、
 * 攻击附毒；靠近花草时获得飞行；按咒石键释放主动技能（消耗经验、治疗并给周围生物再生）。
 *
 * 必须继承 EL 的 {@link ItemSpellstoneBauble}：EL 的按键分发（getAdvancedBaubles）与免疫/抗性消费
 * 都以 {@code instanceof ItemSpellstoneBauble} 硬判定。因此注册域随基类落在 enigmaticlegacy，
 * 模型由 EAddonsMod 用 ModelLoader 单独指到 eaddons 域，纹理/语言仍归属本模组。
 */
public class ItemRevivalLeaf extends EAddonsSpellstoneBauble {
    public static final ItemRevivalLeaf INSTANCE = new ItemRevivalLeaf();

    /** 每个玩家离开植物后的飞行宽限计数。仅服务端主线程访问。 */
    private final Map<EntityPlayer, Integer> flyMap = new WeakHashMap<>();

    public ItemRevivalLeaf() {
        super("revival_leaf", EnumRarity.RARE);

        // 凋零免疫
        this.immunityList.add(DamageSource.WITHER.damageType);
        // 注意：resistanceList 系数按 amount×系数 应用，>1 即"易伤"。
        // 叶子作为植物，对火与穿刺是弱点：1.20 tooltip 明写 vulnerable，故火 ×2、弹射物 ×1.5
        this.resistanceList.put(DamageSource.IN_FIRE.damageType, () -> 2.0F);
        this.resistanceList.put(DamageSource.ON_FIRE.damageType, () -> 2.0F);
        this.resistanceList.put(DamageSource.LAVA.damageType, () -> 2.0F);
        this.resistanceList.put(DamageSource.HOT_FLOOR.damageType, () -> 2.0F);
        this.resistanceList.put("fireball", () -> 2.0F);
        // 生物弹射物易伤：1.12.2 用 arrow/thrown 近似 1.20 的 MOB_PROJECTILE
        this.resistanceList.put("arrow", () -> 1.5F);
        this.resistanceList.put("thrown", () -> 1.5F);
    }

    public int getCooldown(EntityPlayer player) {
        return RevivalLeafConfig.cooldown;
    }

    // ============================ 主动技能（按咒石键触发） ============================

    @Override
    public void triggerActiveAbility(World world, EntityPlayerMP player, ItemStack stack) {
        if (IForbiddenConsumed.get(player).getSpellstoneCooldown() > 0) return;

        int level = ExperienceHelper.getPlayerXPLevel(player);
        int playerXP = ExperienceHelper.getPlayerXP(player);
        if (playerXP <= 10) return;

        ExperienceHelper.drainPlayerXP(player,
                Math.min(MathHelper.ceil(5 * player.getRNG().nextFloat()) + level, playerXP));
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 1.0F, (float) (0.8 + Math.random() * 0.2));
        IForbiddenConsumed.get(player).setSpellstoneCooldown(getCooldown(player));

        List<EntityLivingBase> mobs = world.getEntitiesWithinAABB(EntityLivingBase.class,
                SuperpositionHandler.getBoundingBoxAroundEntity(player, RevivalLeafConfig.abilityRadius));
        for (EntityLivingBase mob : mobs) {
            if (level > 25) {
                mob.heal(Math.min(0.2F, (level - 25) * 0.01F) * mob.getMaxHealth());
            }
            int regenTime = RevivalLeafConfig.regenerationTime
                    + Math.min(playerXP * level / 2, RevivalLeafConfig.regenerationTime);
            mob.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, regenTime,
                    RevivalLeafConfig.regenerationLevel, false, true));
        }
        // 1.20 顺带把凋零玫瑰→虞美人；1.12.2 无凋零玫瑰，略去
    }

    // ============================ 佩戴时被动 ============================

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        super.onWornTick(stack, entity); // 应用（空的）属性修饰符

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            World world = player.world;

            removeNegativeEffects(player);

            if (!hasFlightScroll(player)) {
                boolean flying = !player.isSpectator() && !player.capabilities.isCreativeMode
                        && player.capabilities.isFlying;
                if (flying && hasPlantBy(player)) {
                    player.motionY -= 0.012; // 靠植物飞行时的轻微下坠
                }
                if (!world.isRemote) {
                    handleFlight(player);
                }
            }

            if (!world.isRemote) {
                scanNearbyPoison(player);
                growNearbyCrops(player);
            }
        }

        // 自然回血：对任意佩戴者生效
        if (entity.ticksExisted % RevivalLeafConfig.naturalRegenerationTick == 0
                && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(Math.max(0.5F, entity.getMaxHealth() / 100.0F));
        }
    }

    /** 清除自身 饥饿/凋零/中毒。 */
    private static void removeNegativeEffects(EntityPlayer player) {
        if (player.getActivePotionEffects().isEmpty()) return;
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            if (effect.getPotion() == MobEffects.HUNGER
                    || effect.getPotion() == MobEffects.WITHER
                    || effect.getPotion() == MobEffects.POISON) {
                player.removePotionEffect(effect.getPotion());
            }
        }
    }

    /**
     * 给佩戴者附近处于中毒状态的生物打上 RevivingPoisoned 标记，使其治疗量降至 25%
     * （由 {@link RevivalLeafEventHandler} 读取；解标记也在那里，当目标不再中毒时清除）。
     * 与 tooltip「攻击目标中毒并降低其治疗」一致——标记打在中毒目标而非佩戴者。
     */
    private static void scanNearbyPoison(EntityPlayer player) {
        List<EntityLivingBase> nearby = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, player.getEntityBoundingBox().grow(5.0));
        for (EntityLivingBase living : nearby) {
            if (living.isEntityAlive() && living.isPotionActive(MobEffects.POISON)) {
                living.getEntityData().setBoolean("RevivingPoisoned", true);
            }
        }
    }

    /** 加速附近作物/茎的生长。带区块守卫，未加载即跳过。 */
    private void growNearbyCrops(EntityPlayer player) {
        BlockPos center = player.getPosition();
        World world = player.world;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-3, -1, -3), center.add(3, 1, 3))) {
            if (!world.isBlockLoaded(pos)) continue;
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof BlockCrops) {
                BlockCrops crop = (BlockCrops) block;
                if (!crop.isMaxAge(state) && world.rand.nextInt(16) == 0) {
                    crop.updateTick(world, pos.toImmutable(), state, world.rand);
                }
            } else if (block instanceof BlockStem) {
                if (world.rand.nextInt(16) == 0) {
                    block.updateTick(world, pos.toImmutable(), state, world.rand);
                }
            }
        }
    }

    // ============================ 靠植物飞行 ============================

    /** 标记"当前 allowFlying 是复苏之叶授予的"，收回时只认此标记，避免误关其他模组的创造飞行。 */
    private static final String FLIGHT_OWNED_KEY = "RevivalFlightGranted";

    private void handleFlight(EntityPlayer player) {
        if (hasPlantBy(player)) {
            // 仅当此刻没有任何来源提供飞行时才授予并"认领"；
            // 若 allowFlying 已为真（创造/其他模组授予），飞行归别人管，本物品不接管也不收回
            if (!player.capabilities.allowFlying) {
                player.capabilities.allowFlying = true;
                player.getEntityData().setBoolean(FLIGHT_OWNED_KEY, true);
                syncAbilities(player);
            }
            flyMap.put(player, 5);
        } else {
            int remaining = flyMap.getOrDefault(player, 0);
            if (remaining > 1) {
                flyMap.put(player, remaining - 1);
            } else if (remaining == 1) {
                revokeFlight(player);
                flyMap.put(player, 0);
            }
        }
    }

    /** 只收回本物品自己授予的飞行；他源（创造/旁观/其他模组）的飞行一概不动。 */
    private static void revokeFlight(EntityPlayer player) {
        boolean owned = player.getEntityData().getBoolean(FLIGHT_OWNED_KEY);
        player.getEntityData().removeTag(FLIGHT_OWNED_KEY);
        player.getEntityData().removeTag("RevivalFlightLazyPos");
        if (owned && !player.capabilities.isCreativeMode && !player.isSpectator()) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            syncAbilities(player);
        }
    }

    private static void syncAbilities(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendPlayerAbilities();
        }
    }

    /** 玩家 5 格范围内是否有可支撑飞行的植物（花/树苗/草等），带惰性缓存与区块守卫。 */
    private boolean hasPlantBy(EntityPlayer player) {
        World world = player.world;
        double reachSq = Math.pow(6.0, 2); // 约等于 1.20 的 (reach+1)^2

        if (player.getEntityData().hasKey("RevivalFlightLazyPos")) {
            BlockPos lazy = BlockPos.fromLong(player.getEntityData().getLong("RevivalFlightLazyPos"));
            if (world.isBlockLoaded(lazy) && isPlant(world.getBlockState(lazy))
                    && player.getDistanceSqToCenter(lazy) < reachSq) {
                return true;
            }
        }

        BlockPos base = player.getPosition();
        for (BlockPos pos : BlockPos.getAllInBoxMutable(base.add(-5, -5, -5), base.add(5, 5, 5))) {
            if (!world.isBlockLoaded(pos)) continue;
            if (isPlant(world.getBlockState(pos)) && player.getDistanceSqToCenter(pos) < reachSq) {
                player.getEntityData().setLong("RevivalFlightLazyPos", pos.toImmutable().toLong());
                return true;
            }
        }
        return false;
    }

    /** 近似 1.20 的 FLOWERS/SAPLINGS/REPLACEABLE_BY_TREES 标签集：按方块类与材质判定。 */
    private static boolean isPlant(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockFlower || block instanceof BlockSapling
                || block instanceof BlockDoublePlant || block instanceof BlockTallGrass
                || block instanceof BlockBush) {
            return true;
        }
        Material material = state.getMaterial();
        return material == Material.PLANTS || material == Material.VINE;
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase entity) {
        super.onUnequipped(stack, entity);
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            revokeFlight(player);
            flyMap.put(player, 0);
        }
    }

    // ============================ 辅助 ============================

    private static boolean hasFlightScroll(EntityPlayer player) {
        return isEquipped(player, "fabulous_scroll") || isEquipped(player, "heaven_scroll");
    }

    private static boolean isEquipped(EntityPlayer player, String elItemName) {
        net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(
                new net.minecraft.util.ResourceLocation("enigmaticlegacy", elItemName));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.revival_leaf.desc1"));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.revival_leaf.desc2"));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.revival_leaf.desc3"));
            list.add(TextFormatting.GREEN + I18n.format("tooltip.eaddons.revival_leaf.desc4"));
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.revival_leaf.cooldown",
                    RevivalLeafConfig.cooldown / 20.0F));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.revival_leaf.brief"));
            list.add(I18n.format("tooltip.eaddons.revival_leaf.hold_shift"));
        }
        list.add("");
    }
}
