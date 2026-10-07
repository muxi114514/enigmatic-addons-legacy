package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * 超维封印载体：移植自 1.20 神遗拓展的 PermanentItemEntity + MixinPermanentItemEntity。
 * 战斗模式大招把敌人封印进一个漂浮发光、外观与「超维之眼」相同的实体，
 * 存住该生物完整 NBT 与解封计时；到点后在原地放出生物（附带虚弱与微量虚空伤害）。
 *
 * 刻意继承 {@link Entity} 而非 EntityItem：物理掉落（ItemPhysic）只接管 EntityItem 的
 * 拾取/物理逻辑，换掉基类即可彻底脱离其管辖，从根源避免封印眼被右键/蹲下捡走。
 * 外观由 {@code RenderExtradimensionalLock} 自绘超维之眼物品模型实现。
 */
public class EntityExtradimensionalLock extends Entity {
    /** 用于向客户端同步渲染所需的物品（超维之眼）。 */
    private static final DataParameter<ItemStack> ITEM =
            EntityDataManager.createKey(EntityExtradimensionalLock.class, DataSerializers.ITEM_STACK);

    /** 被封印生物的完整 NBT（含 "id"，用于 EntityList 重建）。 */
    private NBTTagCompound sealedEntity;
    /** 解封所需 tick 数。 */
    private int lockTimer;
    /** 已存在 tick 数。 */
    private int extradimensionTick;

    public EntityExtradimensionalLock(World world) {
        super(world);
        setSize(0.4F, 0.4F);
        this.isImmuneToFire = true;
        this.noClip = true;
    }

    public EntityExtradimensionalLock(World world, double x, double y, double z, ItemStack eyeStack) {
        this(world);
        this.setPosition(x, y, z);
        setDisplayItem(eyeStack);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(ITEM, ItemStack.EMPTY);
    }

    public void setDisplayItem(ItemStack stack) {
        this.dataManager.set(ITEM, stack.copy());
    }

    public ItemStack getDisplayItem() {
        return this.dataManager.get(ITEM);
    }

    public void seal(NBTTagCompound entityNbt, int timer) {
        this.sealedEntity = entityNbt;
        this.lockTimer = timer;
    }

    @Override
    public void onUpdate() {
        // 无重力悬停：手动维护 prev 位置供插值渲染，不调用会施加移动/碰撞的父级逻辑
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.world.isRemote) {
            for (int i = 0; i < 8; i++) {
                float arc = (float) (this.rand.nextFloat() * Math.PI);
                float y = (this.rand.nextFloat() - 0.8F) * 0.1F;
                this.world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        this.posX, this.posY + 0.4 + y * 4, this.posZ,
                        Math.sin(arc) * 0.6, y, Math.cos(arc) * 0.6);
            }
            return;
        }

        this.extradimensionTick++;
        if (this.extradimensionTick > this.lockTimer) {
            releaseSealed();
        }
    }

    private void releaseSealed() {
        if (this.sealedEntity == null) {
            this.setDead();
            return;
        }
        Entity stored = EntityList.createEntityFromNBT(this.sealedEntity, this.world);
        if (stored == null) {
            this.setDead();
            return;
        }
        stored.setLocationAndAngles(this.posX, this.posY, this.posZ, stored.rotationYaw, stored.rotationPitch);
        stored.motionX = 0;
        stored.motionY = 0;
        stored.motionZ = 0;
        this.world.spawnEntity(stored);

        // 放出时施加微量虚空伤害与长时间虚弱，作为封印的代价
        stored.attackEntityFrom(DamageSource.OUT_OF_WORLD, 0.5F);
        if (stored instanceof EntityLivingBase) {
            ((EntityLivingBase) stored).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 600, 4));
        }

        this.world.playSound(null, this.getPosition(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 0.5F, 0.8F + 0.4F * this.rand.nextFloat());
        ItemExtradimensionalScepter.spawnBanishParticles(this.world,
                stored.posX, stored.posY, stored.posZ, stored.width, stored.height);
        this.setDead();
    }

    // 不可碰撞、不可推动、无重力：纯粹的悬浮展示/计时实体
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        ItemStack display = getDisplayItem();
        if (!display.isEmpty()) {
            compound.setTag("DisplayItem", display.writeToNBT(new NBTTagCompound()));
        }
        if (this.sealedEntity != null) {
            compound.setTag("SealedEntity", this.sealedEntity);
        }
        compound.setInteger("LockTimer", this.lockTimer);
        compound.setInteger("ExtradimensionTick", this.extradimensionTick);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("DisplayItem", 10)) {
            setDisplayItem(new ItemStack(compound.getCompoundTag("DisplayItem")));
        }
        if (compound.hasKey("SealedEntity", 10)) {
            this.sealedEntity = compound.getCompoundTag("SealedEntity");
        }
        this.lockTimer = compound.getInteger("LockTimer");
        this.extradimensionTick = compound.getInteger("ExtradimensionTick");
    }
}
