package net.mx.eaddons.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.spellstone.PlayerSpeedTracker;
import net.mx.eaddons.spellstone.SpellstoneData;
import net.mx.eaddons.spellstone.SpellstoneDamage;
import net.mx.eaddons.spellstone.SpellstoneForm;
import net.mx.eaddons.util.FloatingKickGuard;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 失落引擎的抓钩（移植自 EL+ EngineHook）。
 * 飞出 → 勾中实体则把它拖过来，3 秒后自动收回；勾中方块则把持有者拉过去并挂在绳上，
 * 直到再次右键、跳跃跃出、换下引擎形态或方块被破坏。
 *
 * <p>碰撞只在服务端判定（{@link EngineHookCollision}），状态经数据参数同步给客户端。所有者用 UUID 持久化；玩家当前的活跃钩记在
 * persistentData 上，供再次右键时找到并收回。
 *
 * <p>玩家移动是客户端权威：挂绳的收绳 / 摆荡物理在持有者客户端逐 tick 驱动（{@code EngineRopeController}，
 * 经代理调用），服务端不改玩家速度，只负责绳段同步、悬空防踢、清摔落距离与满级充能。
 */
public class EntityEngineHook extends Entity {

    /** 勾中的实体 ID + 1，0 表示没勾中实体。 */
    private static final DataParameter<Integer> HOOKED_ENTITY =
            EntityDataManager.createKey(EntityEngineHook.class, DataSerializers.VARINT);

    /**
     * 持有者的实体 ID，必须同步：客户端没有 {@code WorldServer.getEntityFromUuid}，
     * 只靠 UUID 查不到人，会导致链条画不出来、客户端的绳索物理也找不到本人。
     */
    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityEngineHook.class, DataSerializers.VARINT);

    /** 绕障后的绳段转折点，服务端算好同步给客户端供其他玩家渲染。 */
    private static final DataParameter<NBTTagCompound> ROPE =
            EntityDataManager.createKey(EntityEngineHook.class, DataSerializers.COMPOUND_TAG);

    /** 钩子状态，服务端判定后同步；客户端不自行判碰撞，免得两端勾中的东西不一致。 */
    private static final DataParameter<Byte> STATE =
            EntityDataManager.createKey(EntityEngineHook.class, DataSerializers.BYTE);

    private static final UUID HOOK_KB_UUID = UUID.fromString("6b3f6a8e-0e1a-4c2b-9d3e-000000000070");

    /** 满级充能所需的最低移动速度（格/tick）：被拉或在荡才充，静止挂着不充。 */
    private static final double CHARGE_MIN_SPEED = 0.5D;

    public enum State { FLYING, HOOKED_IN_BLOCK, HOOKED_IN_ENTITY }

    @Nullable private UUID ownerUUID;
    @Nullable private Entity cachedOwner;
    @Nullable private Entity hookedIn;
    /** 勾中的方块，被破坏就松钩。 */
    @Nullable private BlockPos hookedBlock;
    private State currentState = State.FLYING;
    private int hookedTicks;
    private final HookRope rope = new HookRope();

    public EntityEngineHook(World world) {
        super(world);
        setSize(0.25F, 0.25F);
        this.ignoreFrustumCheck = true;
    }

    public EntityEngineHook(World world, EntityPlayer player) {
        this(world);
        this.ownerUUID = player.getUniqueID();
        this.cachedOwner = player;
        this.dataManager.set(OWNER_ID, player.getEntityId());
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        float cos = MathHelper.cos((float) (-Math.toRadians(yaw) - Math.PI));
        float sin = MathHelper.sin((float) (-Math.toRadians(yaw) - Math.PI));
        this.setLocationAndAngles(player.posX - sin * 0.3D,
                player.posY + player.getEyeHeight() - 0.05D,
                player.posZ - cos * 0.3D, yaw, pitch);
        Vec3d look = player.getLookVec().scale(2.75D);
        this.motionX = look.x;
        this.motionY = look.y;
        this.motionZ = look.z;
        updateRotation();
        SpellstoneData.setPlayerEngineHook(player, this.getUniqueID());
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(HOOKED_ENTITY, 0);
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(ROPE, new NBTTagCompound());
        this.dataManager.register(STATE, (byte) 0);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (HOOKED_ENTITY.equals(key)) {
            int id = this.dataManager.get(HOOKED_ENTITY);
            this.hookedIn = id > 0 ? this.world.getEntityByID(id - 1) : null;
        } else if (this.world.isRemote && ROPE.equals(key)) {
            this.rope.readFromNBT(this.dataManager.get(ROPE));
        } else if (this.world.isRemote && STATE.equals(key)) {
            State[] states = State.values();
            this.currentState = states[MathHelper.clamp(this.dataManager.get(STATE), 0, states.length - 1)];
            if (this.currentState != State.FLYING) {
                this.motionX = this.motionY = this.motionZ = 0.0D;
            }
        }
        super.notifyDataManagerChange(key);
    }

    // ============================ tick ============================

    @Override
    public void onUpdate() {
        super.onUpdate();
        Entity owner = getOwner();
        if (!(owner instanceof EntityLivingBase)) {
            if (!this.world.isRemote) {
                this.setDead();
            }
            return;
        }
        EntityLivingBase living = (EntityLivingBase) owner;
        if (!this.world.isRemote && shouldStop(living)) {
            return;
        }

        switch (this.currentState) {
            case FLYING:
                if (!this.world.isRemote && this.ticksExisted > 100) {
                    this.setDead();   // 飞行超时未命中 → 自动收回
                    return;
                }
                // 重力要先于射线加上，射线才与本 tick 的实际位移一致，否则可能被 move 挡在墙前却判不到勾中
                this.motionY -= 0.005D;
                if (!this.world.isRemote) {
                    checkCollision();
                    if (this.currentState != State.FLYING) {
                        return;   // 本 tick 刚勾中
                    }
                }
                break;
            case HOOKED_IN_BLOCK:
                this.motionX = this.motionY = this.motionZ = 0.0D;
                if (this.world.isRemote) {
                    EAddonsMod.proxy.tickEngineRope(this);
                } else {
                    tickHanging(living);
                }
                return;
            case HOOKED_IN_ENTITY:
                tickHookedEntity();
                return;
            default:
                break;
        }

        this.move(net.minecraft.entity.MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        updateRotation();
        this.motionX *= 0.99D;
        this.motionY *= 0.99D;
        this.motionZ *= 0.99D;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    /** 服务端挂绳维护：方块还在、绳段同步、摔落距离与悬空防踢、满级充能。 */
    private void tickHanging(EntityLivingBase owner) {
        if (isHookedBlockGone()) {
            this.setDead();
            return;
        }
        updateRope(owner);
        // 绳子吊着不算下落：否则荡秋千时每次下摆都累加，切手松钩后按累计高度结算摔伤
        owner.fallDistance = 0.0F;
        if (owner instanceof EntityPlayerMP) {
            FloatingKickGuard.reset((EntityPlayerMP) owner);
        }
        chargeOnPull(owner);
    }

    private void tickHookedEntity() {
        if (this.hookedIn == null) {
            if (!this.world.isRemote) {
                this.setDead();   // 读档后被勾实体引用丢失；客户端则可能只是还没收到同步
            }
            return;
        }
        if (!this.hookedIn.isDead && this.hookedIn.world == this.world) {
            this.setPosition(this.hookedIn.posX,
                    this.hookedIn.posY + this.hookedIn.height * 0.8D, this.hookedIn.posZ);
            if (!this.world.isRemote) {
                applyHookedKnockbackResistance();
                drag();
                if (++this.hookedTicks > 60) {
                    this.setDead();
                }
            }
        } else if (!this.world.isRemote) {
            setHookedEntity(null);
            setState(State.FLYING);
        }
    }

    /** 满级：被拉或摆荡期间持续攒能量，供命中时的 ×2.5 全力冲击。 */
    private void chargeOnPull(EntityLivingBase living) {
        if (PlayerSpeedTracker.speed(living) < CHARGE_MIN_SPEED) {
            return;
        }
        ItemStack main = living.getHeldItemMainhand();
        if (main.getItem() instanceof ItemSpellstoneSword
                && SpellstoneData.isResonatingWith(main, SpellstoneForm.LOST_ENGINE)
                && SpellstoneData.getLevel(main) > 4) {
            SpellstoneData.addEnergy(main, 1);
        }
    }

    /** 勾中的方块被挖掉就松钩；只查已加载区块，不触发区块加载。 */
    private boolean isHookedBlockGone() {
        if (this.hookedBlock == null) {
            return false;
        }
        Chunk chunk = this.world.getChunkProvider().getLoadedChunk(
                this.hookedBlock.getX() >> 4, this.hookedBlock.getZ() >> 4);
        if (chunk == null) {
            return false;
        }
        IBlockState state = chunk.getBlockState(this.hookedBlock);
        return state.getBlock().isAir(state, this.world, this.hookedBlock);
    }

    // ============================ 碰撞（仅服务端） ============================

    private void checkCollision() {
        RayTraceResult result = EngineHookCollision.trace(this, getOwner());
        if (result == null) {
            return;
        }
        if (result.entityHit != null) {
            onHitEntity(result.entityHit);
        } else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            onHitBlock(result);
        }
    }

    private void onHitEntity(Entity target) {
        setHookedEntity(target);
        setState(State.HOOKED_IN_ENTITY);
        this.motionX = this.motionY = this.motionZ = 0.0D;
        Entity owner = getOwner();
        if (owner instanceof EntityLivingBase && target instanceof EntityLivingBase) {
            float damage = ItemSpellstoneSword.getAttackDamage((EntityLivingBase) owner)
                    * (float) net.mx.eaddons.item.SpellstoneFormConfig.engineHookDamageRatio;
            target.attackEntityFrom(SpellstoneDamage.magic(owner), damage);
        }
        playHookSound();
    }

    private void onHitBlock(RayTraceResult result) {
        this.hookedBlock = result.getBlockPos();
        this.setPosition(result.hitVec.x, result.hitVec.y, result.hitVec.z);
        this.motionX = this.motionY = this.motionZ = 0.0D;
        setState(State.HOOKED_IN_BLOCK);
        playHookSound();
    }

    private void playHookSound() {
        this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS, 0.5F, 1.2F + 0.6F * this.rand.nextFloat());
    }

    // ============================ 拖拽 ============================

    /** 把勾住的实体拖向玩家。 */
    private void drag() {
        Entity owner = getOwner();
        if (owner == null || this.hookedIn == null) {
            return;
        }
        Vec3d vec = lenModifier(eyeOf(this.hookedIn), eyeOf(owner));
        this.hookedIn.motionX += vec.x * 0.12D;
        this.hookedIn.motionY += vec.y * 0.16D;
        this.hookedIn.motionZ += vec.z * 0.12D;
        this.hookedIn.velocityChanged = true;
    }

    /** 距离 >1 时取单位向量，否则用原向量，避免近距离抽搐。 */
    private static Vec3d lenModifier(Vec3d from, Vec3d to) {
        Vec3d vec = to.subtract(from);
        return vec.lengthVector() > 1.0D ? vec.normalize() : vec;
    }

    private static Vec3d eyeOf(Entity entity) {
        return new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
    }

    /** 重算绳段，变化了才同步，避免每 tick 发包。 */
    private void updateRope(EntityLivingBase owner) {
        if (this.rope.update(this.world, new Vec3d(this.posX, this.posY, this.posZ), eyeOf(owner))) {
            this.dataManager.set(ROPE, this.rope.writeToNBT());
        }
    }

    /** 服务端同步来的绳段，供渲染其他玩家的链条。 */
    public HookRope getRope() {
        return this.rope;
    }

    // ============================ 状态 ============================

    private void setState(State state) {
        this.currentState = state;
        this.dataManager.set(STATE, (byte) state.ordinal());
    }

    private void setHookedEntity(@Nullable Entity entity) {
        this.hookedIn = entity;
        this.dataManager.set(HOOKED_ENTITY, entity == null ? 0 : entity.getEntityId() + 1);
    }

    /** 给被勾住的实体挂抗击退削减，松开时移除。 */
    private void applyHookedKnockbackResistance() {
        if (!(this.hookedIn instanceof EntityLivingBase)) {
            return;
        }
        IAttributeInstance inst = ((EntityLivingBase) this.hookedIn)
                .getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        if (inst != null && inst.getModifier(HOOK_KB_UUID) == null) {
            inst.applyModifier(new AttributeModifier(HOOK_KB_UUID, "Engine hooked", -0.8D, 2).setSaved(false));
        }
    }

    /** 玩家换下引擎形态、死亡或离得太远 → 收钩。 */
    private boolean shouldStop(EntityLivingBase owner) {
        ItemStack stack = owner.getHeldItemMainhand();
        boolean holding = stack.getItem() instanceof ItemSpellstoneSword
                && SpellstoneData.isResonatingWith(stack, SpellstoneForm.LOST_ENGINE);
        if (!owner.isDead && owner.isEntityAlive() && holding && this.getDistanceSq(owner) <= 4096.0D) {
            return false;
        }
        this.setDead();
        return true;
    }

    @Override
    public void setDead() {
        clearOwnerInfo();
        super.setDead();
    }

    private void clearOwnerInfo() {
        Entity owner = getOwner();
        if (owner instanceof EntityLivingBase) {
            SpellstoneData.setPlayerEngineHook((EntityLivingBase) owner, null);
        }
        if (this.hookedIn instanceof EntityLivingBase) {
            IAttributeInstance inst = ((EntityLivingBase) this.hookedIn)
                    .getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
            if (inst != null && inst.getModifier(HOOK_KB_UUID) != null) {
                inst.removeModifier(HOOK_KB_UUID);
            }
        }
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isDead) {
            return this.cachedOwner;
        }
        // 两端通用：先用同步过来的实体 ID 查
        int id = this.dataManager.get(OWNER_ID);
        if (id >= 0) {
            this.cachedOwner = this.world.getEntityByID(id);
            if (this.cachedOwner != null) {
                return this.cachedOwner;
            }
        }
        // 服务端兜底：重载存档后实体 ID 会变，靠 UUID 找回
        if (this.ownerUUID != null && this.world instanceof WorldServer) {
            this.cachedOwner = ((WorldServer) this.world).getEntityFromUuid(this.ownerUUID);
            if (this.cachedOwner != null) {
                this.dataManager.set(OWNER_ID, this.cachedOwner.getEntityId());
            }
        }
        return this.cachedOwner;
    }

    public State getCurrentState() {
        return this.currentState;
    }

    private void updateRotation() {
        double horizontal = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) (MathHelper.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
        this.rotationPitch = (float) (MathHelper.atan2(this.motionY, horizontal) * 180.0D / Math.PI);
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        if (this.ticksExisted > 10 && player == getOwner() && !player.isHandActive()) {
            this.setDead();
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasUniqueId("HookOwner")) {
            this.ownerUUID = tag.getUniqueId("HookOwner");
        }
        setState(State.values()[MathHelper.clamp(tag.getInteger("HookState"), 0, State.values().length - 1)]);
        this.hookedTicks = tag.getInteger("HookTicks");
        if (tag.hasKey("HookBlock")) {
            this.hookedBlock = BlockPos.fromLong(tag.getLong("HookBlock"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (this.ownerUUID != null) {
            tag.setUniqueId("HookOwner", this.ownerUUID);
        }
        tag.setInteger("HookState", this.currentState.ordinal());
        tag.setInteger("HookTicks", this.hookedTicks);
        if (this.hookedBlock != null) {
            tag.setLong("HookBlock", this.hookedBlock.toLong());
        }
    }

    /** 抓钩不应随玩家跨维度。 */
    @Override
    public Entity changeDimension(int dimension) {
        this.setDead();
        return null;
    }

    public EnumHand getHand() {
        return EnumHand.MAIN_HAND;
    }
}
