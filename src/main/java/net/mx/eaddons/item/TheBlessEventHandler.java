package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.mx.eaddons.compat.ModCompat;
import net.mx.eaddons.potion.PotionIchorCorrosion;

import java.lang.reflect.Field;

/**
 * Event handler for The Bless weapon effects:
 * - Fourth curse fix: always deal full damage when holding The Bless
 * - Fire bonus: extra damage to burning targets
 * - Ichor corrosion damage amplification: +10% per level
 * - Fire immunity and extended invulnerability frames
 * - 放在古旧书袋里：命中施加灵液腐蚀、免火、延长无敌三项生效，攻击相关的其余效果仍需手持
 */
public class TheBlessEventHandler {

    private static final int EXTRA_INVULN_TICKS = 10;

    private static float cachedMonsterDamageDebuff = -1F;
    private static Field entityFireField;

    static {
        try {
            entityFireField = Entity.class.getDeclaredField("fire");
            entityFireField.setAccessible(true);
        } catch (Exception e) {
            try {
                entityFireField = Entity.class.getDeclaredField("field_70151_c");
                entityFireField.setAccessible(true);
            } catch (Exception e2) {
                entityFireField = null;
            }
        }
    }

    private static float getMonsterDamageDebuff() {
        if (cachedMonsterDamageDebuff >= 0) return cachedMonsterDamageDebuff;
        try {
            Class<?> configClass = Class.forName("keletu.enigmaticlegacy.EnigmaticConfigs");
            Field field = configClass.getField("monsterDamageDebuff");
            cachedMonsterDamageDebuff = field.getFloat(null);
        } catch (Exception e) {
            cachedMonsterDamageDebuff = 0.5F;
        }
        return cachedMonsterDamageDebuff;
    }

    private static boolean hasCursedRing(EntityPlayer player) {
        try {
            return keletu.enigmaticlegacy.event.SuperpositionHandler.hasCursed(player);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isHoldingTheBless(EntityPlayer player) {
        ItemStack mainhand = player.getHeldItemMainhand();
        return !mainhand.isEmpty() && mainhand.getItem() instanceof ItemTheBless;
    }

    private static boolean isHoldingTheBlessEitherHand(EntityPlayer player) {
        ItemStack mainhand = player.getHeldItemMainhand();
        ItemStack offhand = player.getHeldItemOffhand();
        return (!mainhand.isEmpty() && mainhand.getItem() instanceof ItemTheBless)
                || (!offhand.isEmpty() && offhand.getItem() instanceof ItemTheBless);
    }

    private static boolean isBlessInBag(EntityPlayer player) {
        return ItemAntiqueBag.hasItemInBag(player, ItemTheBless.INSTANCE);
    }

    /** 免火与延长无敌：任一手持有，或放在古旧书袋里。书袋查询要反序列化袋内物品，放在最后判。 */
    private static boolean hasBlessProtection(EntityPlayer player) {
        return isHoldingTheBlessEitherHand(player) || isBlessInBag(player);
    }

    private static int getFireTicks(Entity entity) {
        if (entityFireField != null) {
            try {
                return entityFireField.getInt(entity);
            } catch (Exception ignored) {
            }
        }
        return entity.isBurning() ? 200 : 0;
    }

    /**
     * Cancel fire/lava damage for players holding The Bless.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        if (!event.getSource().isFireDamage()) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (hasBlessProtection(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * At LOW priority (after EnigmaticEvents NORMAL), undo the 4th curse monster damage debuff
     * for players holding The Bless. Also apply fire bonus damage.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        Entity source = event.getSource().getTrueSource();
        Entity immediate = event.getSource().getImmediateSource();
        if (!(source instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) source;
        if (!isHoldingTheBless(player)) return;

        EntityLivingBase target = event.getEntityLiving();

        // Fire damage bonus: scale with target's remaining fire ticks
        if (target.isBurning()) {
            int fireTicks = getFireTicks(target);
            float fireBonus = Math.min(1.0F, fireTicks * 0.0015F);
            event.setAmount(event.getAmount() * (1.0F + fireBonus));
        }

        // Fourth curse fix: undo the monster damage debuff applied by EnigmaticEvents
        // The debuff is applied when: hasCursed AND (immediate != player OR weapon not whitelisted)
        // Since TheBless is not in the whitelist, the debuff was applied. Undo it.
        if (hasCursedRing(player) && immediate == player) {
            boolean isMonster = target instanceof EntityMob || target instanceof EntityDragon
                    || target instanceof EntityWither;
            if (isMonster) {
                float debuff = getMonsterDamageDebuff();
                if (debuff > 0 && debuff < 1) {
                    event.setAmount(event.getAmount() / (1.0F - debuff));
                }
            }
        }
    }

    /**
     * Handle ichor corrosion damage amplification.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDamage(LivingDamageEvent event) {
        // First Aid 按部位结算时每个护甲槽都会再发本事件，这类玩家的增伤已在下面的 LivingHurtEvent 乘过
        if (event.getEntityLiving().world.isRemote || ModCompat.firstAidTakesOver(event.getEntityLiving())) return;
        event.setAmount(amplifyIchor(event.getEntityLiving(), event.getAmount()));
    }

    /**
     * 装了 First Aid 时玩家受害者（PvP）的腐蚀增伤在这里做，每次受伤一次。
     * 打怪仍走上面的 LivingDamageEvent，数值不变；这里是护甲前增伤，对有护甲的玩家略强于护甲后。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onIchorPlayerHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote || !ModCompat.firstAidTakesOver(event.getEntityLiving())) return;
        event.setAmount(amplifyIchor(event.getEntityLiving(), event.getAmount()));
    }

    /** 灵液腐蚀：每级受到的伤害 +10%。 */
    private static float amplifyIchor(EntityLivingBase victim, float amount) {
        PotionEffect corrosion = victim.getActivePotionEffect(PotionIchorCorrosion.INSTANCE);
        if (corrosion == null) return amount;
        return amount * (1.0F + (corrosion.getAmplifier() + 1) * 0.1F);
    }

    /**
     * 受伤后延长无敌时间。放在 LivingHurtEvent 而不是 LivingDamageEvent：First Aid 在 LivingHurtEvent 的
     * LOWEST 接管玩家伤害，之后按护甲槽分轮发 LivingDamageEvent，一次受伤可能发多次。
     * 原版在调用本事件之前已把 hurtResistantTime 设为最大值，这里取较大值不会被覆盖。
     */
    @SubscribeEvent
    public void onBlessHolderHurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote || !(victim instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) victim;
        if (hasBlessProtection(player)) {
            player.hurtResistantTime = Math.max(player.hurtResistantTime,
                    player.maxHurtResistantTime + EXTRA_INVULN_TICKS);
        }
    }

    /**
     * 书袋里的恩惠之典：玩家造成伤害后给目标施加灵液腐蚀 I（5 秒）。
     * 与书袋里的启示之证一致按伤害的真正来源判定，弹射物命中也算。
     * 取 LOWEST，排在上面 LOW 的腐蚀增伤之后——与手持时 hitEntity「命中后才施加」的时序一致，
     * 这一击本身不吃增伤。手持时两边都施加也无妨，同效果同等级只会合并时长。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBagBlessHit(LivingDamageEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote || event.getAmount() <= 0) return;

        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayer) || source == target) return;

        if (isBlessInBag((EntityPlayer) source)) {
            target.addPotionEffect(new PotionEffect(PotionIchorCorrosion.INSTANCE, 100, 0));
        }
    }

    /**
     * Fire immunity: clear fire every tick while holding The Bless.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        if (player.isBurning() && hasBlessProtection(player)) {
            player.extinguish();
        }
    }
}
