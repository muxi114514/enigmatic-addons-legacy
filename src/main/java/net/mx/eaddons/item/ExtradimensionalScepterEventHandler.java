package net.mx.eaddons.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * 超维权杖的全局处理器，移植自 1.20 ExtradimensionalScepter#onTick（LivingTickEvent）。
 * 负责：为每个带有超维值（ExtradimensionCounter）的生物做衰减；当超维值超过阈值时触发封印大招，
 * 把周围最多 MaxCombatCount 个高血量敌人封印进 {@link EntityExtradimensionalLock}。
 */
public class ExtradimensionalScepterEventHandler {
    private static final String COUNTER_TAG = "ExtradimensionCounter";

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (world.isRemote || !entity.isEntityAlive()) return;

        NBTTagCompound data = entity.getEntityData();
        int counter = data.getInteger(COUNTER_TAG);
        if (counter <= 0) return;

        int threshold = Math.max(MathHelper.floor(entity.getHealth() * 20), 200);
        if (counter > threshold) {
            banish(world, entity, threshold);
            return;
        }

        counter = Math.max(0, counter - Math.max(1, MathHelper.ceil(Math.pow(threshold, 0.3))));
        if (counter == 0) {
            data.removeTag(COUNTER_TAG);
        } else {
            data.setInteger(COUNTER_TAG, counter);
        }
    }

    /** 封印大招：把周围最多 MaxCombatCount 个高血量敌人（含触发者自身）封印进超维之眼。 */
    private void banish(World world, EntityLivingBase trigger, int threshold) {
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class,
                trigger.getEntityBoundingBox().grow(10));
        nearby.removeIf(living -> !ItemExtradimensionalScepter.Helper.validTarget(trigger, living, 1.4));
        nearby.sort((a, b) -> Float.compare(b.getHealth(), a.getHealth()));

        int limit = Math.min(ExtradimensionalScepterConfig.maxCombatCount, nearby.size());
        List<EntityLivingBase> sealed = new java.util.ArrayList<>(nearby.subList(0, limit));
        if (!sealed.contains(trigger)) sealed.add(trigger);

        world.playSound(null, trigger.getPosition(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 0.5F, 0.8F + 0.4F * trigger.getRNG().nextFloat());

        int lockTimer = MathHelper.floor(Math.sqrt(threshold - 200) * 4) + 120;
        Item eye = ItemExtradimensionalScepter.getEye();
        ItemStack eyeStack = new ItemStack(eye != null ? eye : Items.ENDER_EYE);

        for (EntityLivingBase target : sealed) {
            target.getEntityData().removeTag(COUNTER_TAG);

            NBTTagCompound entityTag = new NBTTagCompound();
            target.writeToNBTOptional(entityTag);
            entityTag.removeTag("UUIDMost");
            entityTag.removeTag("UUIDLeast");

            // 放出时以更低血量返回：min(0.9×当前血量, 当前血量 - 反击伤害)，最低 0.5
            float damage = 0.0F;
            EntityLivingBase lastAttacker = target.getRevengeTarget();
            if (lastAttacker != null
                    && lastAttacker.getHeldItemMainhand().getItem() == ItemExtradimensionalScepter.INSTANCE) {
                damage = (float) (lastAttacker.getEntityAttribute(
                        net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() / 2.0);
            }
            float newHealth = Math.min(target.getHealth() * 0.9F, target.getHealth() - damage);
            entityTag.setFloat("Health", Math.max(newHealth, 0.5F));

            EntityExtradimensionalLock lock = new EntityExtradimensionalLock(
                    world, target.posX, target.posY + target.height * 0.5, target.posZ, eyeStack.copy());
            lock.seal(entityTag, lockTimer);
            world.spawnEntity(lock);

            ItemExtradimensionalScepter.spawnBanishParticles(world,
                    target.posX, target.posY, target.posZ, target.width, target.height);
            target.setDead();
        }
    }
}
