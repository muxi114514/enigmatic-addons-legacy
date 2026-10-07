package net.mx.eaddons.spellstone;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.mx.eaddons.item.SpellstoneFormConfig;
import net.mx.eaddons.item.SpellstoneSwordConfig;

import java.util.List;

/**
 * 各共鸣形态的主动技能实现。
 *
 * <p>与 1.20.1 的差异：海洋形态整条重做（激流冲刺依赖 1.13 的三叉戟自旋，1.12.2 没有），
 * 改为「命中攒能量、满能量落雷」；天使光束与幻影魂焰弹在 P1 先用射线判定实现，
 * P3 再换成可见的投射物实体。
 */
final class SpellstoneActives {

    private SpellstoneActives() {
    }

    // ============================ 烈焰之核 ============================

    /** 长按喷火：每 5 tick 扣能量，对前方锥形造成岩浆伤并击退。 */
    static void blazingFlameTick(World world, EntityPlayer player, ItemStack stack) {
        if (world instanceof WorldServer) {
            Vec3d look = player.getLookVec();
            ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                    player.posX + look.x, player.posY + player.getEyeHeight() + look.y, player.posZ + look.z,
                    8, 0.4D, 0.4D, 0.4D, 0.03D);
        }
        if (player.ticksExisted % 5 != 0) {
            return;
        }
        if (!SpellstoneData.isCreative(player)) {
            SpellstoneData.addEnergy(stack, -SpellstoneSwordConfig.blazingFlameCost);
            if (SpellstoneData.getEnergy(stack) <= 0) {
                player.resetActiveHand();
                return;
            }
        }
        float damage = SpellstoneAbilities.attackDamage(player) * (float) SpellstoneFormConfig.blazingFlameRatio;
        for (EntityLivingBase target : SpellstoneAbilities.getConeTargets(player, 3.0D, 0.32D)) {
            target.attackEntityFrom(SpellstoneDamage.lava(player), damage);
            target.hurtResistantTime = 4;
            double dx = player.posX - target.posX;
            double dz = player.posZ - target.posZ;
            target.knockBack(player, 0.2F, dx, dz);
        }
    }

    /** 右键方块：能量为 0 时吸热充满，有能量时岩浆爆。 */
    static EnumActionResult blazingUseOn(World world, EntityPlayer player, ItemStack stack,
                                         BlockPos pos, float hitX, float hitY, float hitZ) {
        int energy = SpellstoneData.getEnergy(stack);
        if (energy == 0) {
            return blazingAbsorb(world, player, stack, pos);
        }
        if (player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        int level = SpellstoneData.getLevel(stack);
        SwordCooldown.set(player, stack, Math.max(1, 40 - level * 2));
        if (!SpellstoneData.isCreative(player)) {
            SpellstoneData.addEnergy(stack, -SpellstoneSwordConfig.blazingBurstCost);
        }
        float damage = SpellstoneAbilities.attackDamage(player) * (float) SpellstoneFormConfig.blazingBurstRatio;
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class,
                new net.minecraft.util.math.AxisAlignedBB(pos).grow(2.0D));
        for (EntityLivingBase target : list) {
            if (!AoeTargets.canHit(player, target)) {
                continue;
            }
            target.attackEntityFrom(SpellstoneDamage.lava(player), damage);
            target.knockBack(player, 1.2F, pos.getX() + 0.5D - target.posX, pos.getZ() + 0.5D - target.posZ);
            target.setFire(8);
        }
        if (!player.onGround) {
            player.motionY = 0.4D;
            player.velocityChanged = true;
        }
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA,
                    pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ, 20, 1.0D, 1.0D, 1.0D, 0.1D);
        }
        return EnumActionResult.SUCCESS;
    }

    /**
     * 从周围 3 格内的火 / 岩浆 / 岩浆块吸热充能。
     * <p>这里要逐格读方块，按项目规范先确认覆盖的区块都已在内存中，避免触发同步加载卡住主线程。
     */
    private static EnumActionResult blazingAbsorb(World world, EntityPlayer player, ItemStack stack, BlockPos pos) {
        final int r = 3;
        for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
            for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk == null) {
                    return EnumActionResult.PASS;
                }
            }
        }
        int heat = 0;
        for (BlockPos bp : BlockPos.getAllInBoxMutable(pos.add(-r, -r, -r), pos.add(r, r, r))) {
            IBlockState state = world.getBlockState(bp);
            if (state.getBlock() == Blocks.FIRE) {
                heat++;
                world.setBlockToAir(bp);
            } else if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) {
                heat += 9;
            } else if (state.getBlock() == Blocks.MAGMA) {
                heat += 2;
            }
            if (heat > 8) {
                SwordCooldown.set(player, stack, 10);
                SpellstoneData.setEnergy(stack, SpellstoneData.getMaxEnergy(stack));
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 24, 0.5D, 0.5D, 0.5D, 0.05D);
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    // ============================ 海洋意志（本次重做）============================

    /**
     * 能量满时对注视的敌人降下落雷：闪电用 effectOnly 构造，只出视觉与雷声，
     * 不引燃方块因而不会烧掉地上的掉落物，伤害由自己按攻击力结算。
     */
    static void oceanLightning(World world, EntityPlayer player, ItemStack stack) {
        List<EntityLivingBase> observed = SpellstoneAbilities.getObservedEntities(player, world, 2.0D, 24);
        EntityLivingBase target = observed.isEmpty() ? null : observed.get(0);
        if (target == null) {
            return;
        }
        SpellstoneData.setEnergy(stack, 0);
        SwordCooldown.set(player, stack, 20);

        if (!world.isRemote) {
            world.addWeatherEffect(new EntityLightningBolt(world, target.posX, target.posY, target.posZ, true));
        }
        float damage = SpellstoneAbilities.attackDamage(player) * (float) SpellstoneSwordConfig.oceanLightningRatio;
        target.attackEntityFrom(SpellstoneDamage.lightning(player), damage);
        stack.damageItem(1, player);
    }

    // ============================ 星云之眼 ============================

    /** 潜行右键：有同维度锚点则传送回去。返回是否真的传送了。 */
    static boolean nebulaReturn(World world, EntityPlayer player, ItemStack stack) {
        if (!SpellstoneData.hasAnchor(stack)) {
            return false;
        }
        if (SpellstoneData.getAnchorDim(stack) != world.provider.getDimension()) {
            SpellstoneData.clearAnchor(stack);
            return false;
        }
        BlockPos anchor = SpellstoneData.getAnchorPos(stack);
        if (anchor == null) {
            return false;
        }
        player.setPositionAndUpdate(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                player.getSoundCategory(), 1.0F, 1.25F);
        spawnPortal(world, player.posX, player.posY, player.posZ, 32);
        SpellstoneData.clearAnchor(stack);
        return true;
    }

    /** 蓄满后松手：与注视方向最远的敌人换位，造成魔法伤并横扫；满级沿路径 AOE。 */
    static void nebulaTeleportSlash(World world, EntityPlayer player, ItemStack stack, int elapsed) {
        int level = SpellstoneData.getLevel(stack);
        // 蓄力越久看得越远，封顶 32 格（按住不放时 elapsed 会一直涨）
        List<EntityLivingBase> observed = SpellstoneAbilities.getObservedEntities(player, world, 2.0D,
                16 + Math.min(elapsed, 64) / 4);
        observed.removeIf(e -> e.width * e.height > player.width * player.height * 5.0D);
        if (observed.isEmpty()) {
            world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    player.getSoundCategory(), 0.6F, 0.25F);
            return;
        }
        EntityLivingBase target = observed.get(observed.size() - 1);
        double tx = target.posX, ty = target.posY, tz = target.posZ;
        double sx = player.posX, sy = player.posY, sz = player.posZ;

        double value = SpellstoneAbilities.attackDamage(player);
        double dist = Math.sqrt((tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) + (tz - sz) * (tz - sz));
        double ratio = SpellstoneFormConfig.nebulaBlinkRatio + (level > 4 ? dist * 0.02D : 0.0D);
        float damage = (float) (value * ratio);
        target.attackEntityFrom(SpellstoneDamage.magic(player), damage);
        stack.damageItem(1, player);

        target.setPositionAndUpdate(sx, sy, sz);
        player.setPositionAndUpdate(tx, ty, tz);
        lookAt(player, sx, sy + player.getEyeHeight(), sz);
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                player.getSoundCategory(), 1.0F, 1.25F);
        SweepHelper.sweep(player, 0.0F, 3.0D);
        player.swingArm(EnumHand.MAIN_HAND);

        // 传送路径的粒子；满级时沿途再打一次范围魔法伤
        double len = Math.max(1.0D, dist / 1.25D);
        for (int i = 0; i < len; i++) {
            double x = (sx * i + tx * (len - i)) / len;
            double y = (sy * i + ty * (len - i)) / len;
            double z = (sz * i + tz * (len - i)) / len;
            if (level > 4) {
                List<EntityLivingBase> path = world.getEntitiesWithinAABB(EntityLivingBase.class,
                        new net.minecraft.util.math.AxisAlignedBB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1));
                for (EntityLivingBase e : path) {
                    if (AoeTargets.canHit(player, e)) {
                        e.attackEntityFrom(SpellstoneDamage.magic(player),
                                (float) (value * SpellstoneFormConfig.nebulaPathRatio));
                    }
                }
            }
            spawnPortal(world, x, y, z, 6);
        }
    }

    // ============================ 天使之祝 / 幻影灯笼 ============================

    /** 潜行 = 前方锥形爆发；站立 = 单点光束。投射物实体属于 P3，这里先用射线判定。 */
    static void angelShoot(World world, EntityPlayer player, ItemStack stack) {
        int level = SpellstoneData.getLevel(stack);
        boolean creative = SpellstoneData.isCreative(player);
        if (player.isSneaking()) {
            if (!creative) {
                stack.damageItem(3, player);
                SpellstoneData.addEnergy(stack, -3);
            }
            SwordCooldown.set(player, stack, 10);
            player.playSound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 0.8F, 1.4F);
            float damage = SpellstoneAbilities.attackDamage(player) * (float) SpellstoneFormConfig.angelBurstRatio;
            for (EntityLivingBase target : SpellstoneAbilities.getConeTargets(player, 4.5D, 0.25D)) {
                target.attackEntityFrom(SpellstoneDamage.magic(player), damage);
                target.knockBack(player, 2.0F, player.posX - target.posX, player.posZ - target.posZ);
                if (level > 4) {
                    SpellstoneAbilities.markBless(target, 4);
                }
            }
            if (world instanceof WorldServer) {
                Vec3d look = player.getLookVec();
                ((WorldServer) world).spawnParticle(EnumParticleTypes.END_ROD,
                        player.posX + look.x, player.posY + player.getEyeHeight() + look.y, player.posZ + look.z,
                        40, 0.5D, 0.5D, 0.5D, 0.1D);
            }
        } else {
            if (!creative) {
                stack.damageItem(1, player);
                SpellstoneData.addEnergy(stack, -1);
            }
            SwordCooldown.set(player, stack, 6);
            player.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.6F);
            if (!world.isRemote) {
                world.spawnEntity(new net.mx.eaddons.entity.EntityAngelBeam(world, player, 2.4F,
                        SpellstoneAbilities.attackDamage(player) * (float) SpellstoneFormConfig.angelBeamRatio, level > 4));
            }
        }
    }

    /** 幻影魂焰弹，同样先用射线，P3 换成追踪实体。 */
    static void illusionShoot(World world, EntityPlayer player, ItemStack stack, EnumHand hand) {
        player.swingArm(hand);
        SwordCooldown.set(player, stack, 5);
        if (!SpellstoneData.isCreative(player)) {
            SpellstoneData.addEnergy(stack, -1);
        }
        player.playSound(SoundEvents.ITEM_FIRECHARGE_USE, 1.0F, 1.0F);
        // 复用幻影灵魂灯笼那套魂焰弹：它「已点火」的状态本就是追踪飞行，
        // 命中效果也正好是叠「幻影魂」层数，只需跳过环绕轨道直接射出去
        if (!world.isRemote) {
            world.spawnEntity(new net.mx.eaddons.item.EntitySoulFlameBall(world, player,
                    player.getLookVec().scale(1.6D),
                    SpellstoneAbilities.attackDamage(player) * (float) SpellstoneFormConfig.illusionFlameRatio));
        }
    }

    // ============================ 失落引擎 ============================

    /**
     * 抓钩开关：有活跃钩就收回，否则放一个新的。
     * <p>钩的 UUID 只存在服务端玩家 persistentData，客户端看不到，
     * 所以两端不能各自分支，逻辑全在服务端、客户端只回报成功。
     */
    static boolean handleHook(World world, EntityPlayer player, ItemStack stack) {
        if (world.isRemote) {
            return true;
        }
        if (retractHook(player)) {
            return true;
        }
        world.spawnEntity(new net.mx.eaddons.entity.EntityEngineHook(world, player));
        stack.damageItem(1, player);
        playChainSound(player, 1.0F);
        return true;
    }

    /**
     * 收回玩家当前的抓钩（再次右键、挂绳时跳跃跃出）。
     *
     * @return 确实有钩被收回
     */
    static boolean retractHook(EntityPlayer player) {
        java.util.UUID uuid = SpellstoneData.getPlayerEngineHook(player);
        if (uuid == null || !(player.world instanceof WorldServer)) {
            return false;
        }
        net.minecraft.entity.Entity existing = ((WorldServer) player.world).getEntityFromUuid(uuid);
        if (existing instanceof net.mx.eaddons.entity.EntityEngineHook) {
            existing.setDead();
            playChainSound(player, 0.8F);
            return true;
        }
        SpellstoneData.setPlayerEngineHook(player, null);   // 清掉可能残留的失效 UUID
        return false;
    }

    /** 服务端 player.playSound 会排除玩家本人，这里传 null 让持有者自己也听得到。 */
    private static void playChainSound(EntityPlayer player, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0F, pitch);
    }

    // ============================ 工具 ============================

    private static EntityLivingBase firstObserved(EntityPlayer player, World world, int maxDist) {
        List<EntityLivingBase> list = SpellstoneAbilities.getObservedEntities(player, world, 1.2D, maxDist);
        return list.isEmpty() ? null : list.get(0);
    }

    private static void beamParticles(World world, EntityPlayer player, EntityLivingBase target,
                                      EnumParticleTypes type) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        double sx = player.posX, sy = player.posY + player.getEyeHeight(), sz = player.posZ;
        double dx = target.posX - sx, dy = target.posY + target.height * 0.5D - sy, dz = target.posZ - sz;
        int steps = (int) Math.max(4.0D, Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0D);
        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps;
            ((WorldServer) world).spawnParticle(type, sx + dx * f, sy + dy * f, sz + dz * f,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnPortal(World world, double x, double y, double z, int count) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL, x, y, z,
                    count, 0.2D, 0.4D, 0.2D, 0.03D);
        }
    }

    /** 把玩家朝向转到指定坐标，替代 1.20.1 的 lookAt(EntityAnchorArgument...)。 */
    private static void lookAt(EntityPlayer player, double x, double y, double z) {
        double dx = x - player.posX;
        double dy = y - (player.posY + player.getEyeHeight());
        double dz = z - player.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        player.rotationYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        player.rotationPitch = (float) (-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
        player.setRotationYawHead(player.rotationYaw);
    }
}
