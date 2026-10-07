package net.mx.eaddons.spellstone;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.mx.eaddons.item.FrostHelper;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.item.SpellstoneSwordConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 术质共鸣者的主动技能分发与共用逻辑。各形态的具体实现在 {@link SpellstoneActives}。
 *
 * <p>只在服务端主线程调用（右键包、命中事件、inventoryTick），所有 AOE 都只遍历实体、不读方块，
 * 唯一读写方块的是烈焰形态的吸热与岩浆爆，那两处在 {@link SpellstoneActives} 里做了区块守卫。
 */
public final class SpellstoneAbilities {

    private SpellstoneAbilities() {
    }

    // ============================ 右键主动 ============================

    public static ActionResult<ItemStack> useForm(World world, EntityPlayer player, EnumHand hand,
                                                  ItemStack stack, SpellstoneForm form) {
        int energy = SpellstoneData.getEnergy(stack);
        switch (form) {
            case BLAZING_CORE:
                // 有能量 → 长按喷火，松手横扫
                if (energy > 0) {
                    SwordShieldUse.startSkillUse(player, hand, stack);
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
                break;
            case OCEAN_STONE:
                // 能量满 → 立刻放落雷；否则无主动（能量靠命中与被动积攒）
                if (energy >= SpellstoneData.getMaxEnergy(stack) && SpellstoneData.getMaxEnergy(stack) > 0) {
                    SpellstoneActives.oceanLightning(world, player, stack);
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
                break;
            case VOID_PEARL:
                // 蓄力格挡，弹反判定在 SpellstoneHolderDefense；原初共鸣时举起不进冷却
                SwordShieldUse.startSkillUse(player, hand, stack);
                if (!SpellstoneHolderDefense.parryCooldownWaived(stack)) {
                    SwordCooldown.set(player, stack, 64);
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            case THE_CUBE:
                // 站立折跃、潜行对调
                if (player.isSneaking()) {
                    CubeResonance.swapEffects(world, player, stack);
                } else {
                    CubeResonance.blink(world, player, stack);
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            case PRIMEVAL_CUBE:
                return new ActionResult<>(EnumActionResult.PASS, stack);   // 纯原初没有右键主动
            case ANGEL_BLESSING:
                if (energy > 0) {
                    SpellstoneActives.angelShoot(world, player, stack);
                } else {
                    SwordShieldUse.startSkillUse(player, hand, stack);   // 蓄力充能
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            case ILLUSION_LANTERN:
                if (energy > 0) {
                    SpellstoneActives.illusionShoot(world, player, stack, hand);
                } else {
                    SwordShieldUse.startSkillUse(player, hand, stack);
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            case EYE_OF_NEBULA:
                // 潜行且有同维度锚点 → 传送回去；否则蓄力注视瞬移斩
                if (player.isSneaking() && SpellstoneActives.nebulaReturn(world, player, stack)) {
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
                SwordShieldUse.startSkillUse(player, hand, stack);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            case LOST_ENGINE:
                SpellstoneActives.handleHook(world, player, stack);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            default:
                break;
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    // ============================ 蓄力 ============================

    /** @param elapsed 已蓄力的 tick 数 */
    public static void onChargeTick(EntityPlayer player, ItemStack stack, int elapsed) {
        SpellstoneForm form = SpellstoneData.getForm(stack);
        if (form == SpellstoneForm.BLAZING_CORE) {
            SpellstoneActives.blazingFlameTick(player.world, player, stack);
        } else if (form == SpellstoneForm.EYE_OF_NEBULA && elapsed == 32) {
            player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 1.0F, 0.9F);
        } else if (form == SpellstoneForm.ANGEL_BLESSING && (elapsed == 12 || elapsed == 24)) {
            player.playSound(SoundEvents.BLOCK_NOTE_CHIME, 0.6F, elapsed == 12 ? 0.7F : 1.0F);
        }
    }

    public static void onChargeRelease(World world, EntityPlayer player, ItemStack stack, int elapsed) {
        SpellstoneForm form = SpellstoneData.getForm(stack);
        if (form == SpellstoneForm.BLAZING_CORE && SpellstoneData.getEnergy(stack) > 0) {
            player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            SweepHelper.sweep(player, 0.0F, 3.0D);
            SwordCooldown.set(player, stack, 20);
            player.swingArm(EnumHand.MAIN_HAND);
        } else if (form == SpellstoneForm.EYE_OF_NEBULA && elapsed >= 32) {
            SpellstoneActives.nebulaTeleportSlash(world, player, stack, elapsed);
        }
    }

    /** 天使/幻影蓄力满：补满能量。 */
    public static void onChargeFinish(World world, EntityPlayer player, ItemStack stack) {
        SpellstoneForm form = SpellstoneData.getForm(stack);
        if (form == SpellstoneForm.ANGEL_BLESSING || form == SpellstoneForm.ILLUSION_LANTERN) {
            SpellstoneData.setEnergy(stack, SpellstoneData.getMaxEnergy(stack));
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7F, 1.4F);
        }
    }

    // ============================ 右键方块 ============================

    public static EnumActionResult useOnBlock(World world, EntityPlayer player, ItemStack stack,
                                              BlockPos pos, float hitX, float hitY, float hitZ) {
        SpellstoneForm form = SpellstoneData.getForm(stack);
        if (form == SpellstoneForm.BLAZING_CORE) {
            return SpellstoneActives.blazingUseOn(world, player, stack, pos, hitX, hitY, hitZ);
        }
        if (form == SpellstoneForm.EYE_OF_NEBULA && player.isSneaking() && !SpellstoneData.hasAnchor(stack)) {
            SpellstoneData.setAnchor(stack, world.provider.getDimension(), pos.up());
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    player.getSoundCategory(), 0.6F, 1.2F);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    // ============================ 命中 ============================

    public static void onHitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (attacker.world.isRemote) {
            return;
        }
        // 原初共鸣的命中效果（削无敌帧、纯原初的灵液腐蚀）与子形态无关，先结算
        PrimevalResonance.onHit(stack, target, attacker);
        SpellstoneForm form = SpellstoneData.getForm(stack);
        int level = SpellstoneData.getLevel(stack);
        switch (form) {
            case BLAZING_CORE:
                target.setFire(5 + level);
                HeatHelper.onHit(attacker, level);   // 续自己的炽热；满级前增伤减半，见 SpellstoneSwordEvents
                break;
            case OCEAN_STONE:
                SpellstoneData.addEnergy(stack, SpellstoneSwordConfig.oceanEnergyPerHit);
                break;
            case REVIVAL_LEAF:
                // 能量攒满后，下次命中清空能量并治疗自身 80% 最大生命。
                // 走 heal 而非直接 setHealth，First Aid 的分部位血量才不会被绕开。
                if (SpellstoneData.getMaxEnergy(stack) > 0
                        && SpellstoneData.getEnergy(stack) >= SpellstoneData.getMaxEnergy(stack)) {
                    SpellstoneData.setEnergy(stack, 0);
                    attacker.heal(attacker.getMaxHealth() * 0.8F);
                }
                break;
            case FORGOTTEN_ICE: {
                // 复用忘却冰晶术石那套自建冻结
                int frost = SpellstoneSwordConfig.frostPerHit + 10 * level;
                FrostHelper.applyFrost(target, FrostHelper.frostTimeFor(target, frost));
                // 冰盾按造成的伤害积攒，在拿得到伤害值的 SpellstoneSwordEvents#onLivingDamage 里结算
                // 满级：顺带冻住目标周围的一圈
                if (level > 4) {
                    AxisAlignedBB box = target.getEntityBoundingBox()
                            .grow(SpellstoneSwordConfig.frostAoeRadius);
                    for (EntityLivingBase nearby : attacker.world
                            .getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                        if (nearby == target || !AoeTargets.canHit(attacker, nearby)) {
                            continue;
                        }
                        FrostHelper.applyFrost(nearby, FrostHelper.frostTimeFor(nearby,
                                SpellstoneSwordConfig.frostAoeTime));
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    // ============================ 物品栏 tick ============================

    public static void inventoryTick(World world, EntityPlayer player, ItemStack stack, boolean selected) {
        SpellstoneData.tickConfirm(stack);
        SpellstoneData.tickTimers(stack);
        if (!selected && stack != player.getHeldItemOffhand()) {
            return;
        }
        // 链接式共鸣要跟着「有没有戴着那颗术石」开关，属性修饰符也靠这一步刷新
        ResonanceLink.refresh(player, stack);
        if (!selected) {
            return;
        }
        if (SpellstoneData.isResonatingWith(stack, SpellstoneForm.OCEAN_STONE)
                && player.ticksExisted % SpellstoneSwordConfig.oceanRegenInterval == 0
                && (player.isWet() || world.isThundering())) {
            SpellstoneData.addEnergy(stack, SpellstoneSwordConfig.oceanRegenAmount);
        }
    }

    // ============================ 共用工具 ============================

    /**
     * 沿视线逐段取有视线的实体，按离起点由近到远排序（取最近用 get(0)，取最远用最后一个）。
     * <p>移植自 EL+ 的 getObservedEntities，但原版每一步都把「当前距离」再累加到采样点上，采样点落在约
     * 0.4、1.6、3.6、6.4、10、14.4……格处，越走越稀：十来格外相邻判定框之间就有空隙，站在空隙里的目标
     * 选不中；而且采样一路延伸到几百格外。这里改成等距步进，步长不超过判定框半径，相邻框互相重叠。
     */
    public static List<EntityLivingBase> getObservedEntities(EntityLivingBase from, World world,
                                                             double range, int maxDist) {
        Vec3d start = new Vec3d(from.posX, from.posY + from.height / 2.0D, from.posZ);
        Vec3d look = from.getLookVec();
        double step = Math.max(0.5D, Math.min(1.0D, range));
        Set<EntityLivingBase> found = new LinkedHashSet<>();
        for (double distance = step; distance <= maxDist; distance += step) {
            Vec3d point = start.add(look.scale(distance));
            AxisAlignedBB box = new AxisAlignedBB(
                    point.x - range, point.y - range, point.z - range,
                    point.x + range, point.y + range, point.z + range);
            for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                if (e != from && e.isEntityAlive()) {
                    found.add(e);
                }
            }
        }
        List<EntityLivingBase> entities = new ArrayList<>(found);
        entities.removeIf(e -> !from.canEntityBeSeen(e));
        entities.sort(Comparator.comparingDouble(e -> e.getDistanceSq(from)));
        return entities;
    }

    /** 前方锥形范围内的实体（喷火、天使范围爆发共用）。 */
    public static List<EntityLivingBase> getConeTargets(EntityPlayer player, double radius, double minDot) {
        List<EntityLivingBase> list = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(radius));
        Vec3d look = player.getLookVec();
        list.removeIf(e -> {
            if (!AoeTargets.canHit(player, e)) {
                return true;
            }
            Vec3d delta = new Vec3d(e.posX - player.posX, e.posY - player.posY, e.posZ - player.posZ).normalize();
            return delta.dotProduct(look) < minDot;
        });
        return list;
    }

    /**
     * 天使祝福的三来源位标记：近战 bit1、光束 bit2、范围 bit4，满级按集齐数增伤。
     * <p>放在本类而非 SpellstoneActives，是因为光束实体在别的包里也要标记。
     */
    public static void markBless(EntityLivingBase target, int bit) {
        int bless = target.getEntityData().getInteger("ResonanceAngelBless");
        target.getEntityData().setInteger("ResonanceAngelBless", bless | bit);
    }

    public static float attackDamage(EntityLivingBase user) {
        return ItemSpellstoneSword.getAttackDamage(user);
    }
}
