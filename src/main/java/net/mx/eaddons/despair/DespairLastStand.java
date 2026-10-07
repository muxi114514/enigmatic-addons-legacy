package net.mx.eaddons.despair;

import baubles.api.BaublesApi;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.compat.FirstAidHealth;
import net.mx.eaddons.item.ItemInsigniaOfDespair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * 绝望者证章的「绝境」：致命伤害越过所有免死效果后，生命锁在 1 点并给一段求生时间，
 * 期间击杀敌对生物、或结束时生命不低于阈值即脱险，否则被绝望吞噬而死。
 *
 * <p>触发和「期间不死」都挂在原版图腾检查上（见 {@code MixinEntityLivingTotem}）。First Aid 判定玩家该死时
 * 也会调这个检查，返回 true 后由它把致命部位拉回至少 1 点，一处同时覆盖两条死亡路径；
 * 死亡事件根本不会发出，别的模组不会误以为玩家死过一次。
 */
public final class DespairLastStand {
    public static final String DAMAGE_TYPE = "eaddons.despair";
    /** 失败时的致死伤害：无视护甲与减伤；和 /kill 一样 canHarmInCreative，原版图腾与虚空调谐都不救。 */
    private static final DamageSource DESPAIR = new DamageSource(DAMAGE_TYPE)
            .setDamageBypassesArmor().setDamageIsAbsolute().setDamageAllowedInCreativeMode();
    /** 处决被挡下（如登录后 3 秒无敌、别的模组取消死亡）时逐 tick 重试的上限。 */
    private static final int DOOM_ATTEMPTS = 100;
    private static final Logger LOG = LogManager.getLogger("eaddons");

    private DespairLastStand() {
    }

    public static boolean isDespairDeath(@Nullable DamageSource source) {
        return source != null && DAMAGE_TYPE.equals(source.getDamageType());
    }

    /** 绝境期间的致死伤害一律锁在 1 点生命。排在图腾检查最前面，不会白白消耗图腾。 */
    public static boolean holdAtDeath(EntityLivingBase entity, DamageSource source) {
        if (!(entity instanceof EntityPlayerMP) || source.canHarmInCreative()
                || LastStandData.timer((EntityPlayer) entity) <= 0) {
            return false;
        }
        entity.setHealth(1.0F);
        return true;
    }

    /** 图腾检查的最终结论是「会死」时调用：满足条件就进入绝境，并改判为不死。 */
    public static boolean tryTrigger(EntityLivingBase entity, DamageSource source) {
        if (!(entity instanceof EntityPlayerMP) || !canTrigger((EntityPlayerMP) entity, source)) {
            return false;
        }
        begin((EntityPlayerMP) entity);
        return true;
    }

    /** 服务端每个玩家每 tick 调用：倒计时、结算、处决重试、冷却结束提示。极限模式死后成了旁观者就不再处理。 */
    static void tick(EntityPlayerMP player) {
        NBTTagCompound tag = LastStandData.peek(player);
        if (tag == null || !player.isEntityAlive() || player.isSpectator()) {
            return;
        }
        if (tag.getInteger(LastStandData.DOOM) > 0) {
            doom(player, tag);
            return;
        }
        int timer = tag.getInteger(LastStandData.TIMER);
        if (timer > 0) {
            tag.setInteger(LastStandData.TIMER, --timer);
            if (timer == 0) {
                conclude(player, tag);
            } else if (timer % 10 == 0) {
                sendCountdown(player, timer);
            }
            return;
        }
        long readyAt = tag.getLong(LastStandData.READY_AT);
        if (readyAt != 0L && player.world.getTotalWorldTime() >= readyAt) {
            tag.removeTag(LastStandData.READY_AT);
            LastStandData.removeIfEmpty(player);
            player.sendMessage(new TextComponentTranslation("message.eaddons.last_stand.ready"));
        }
    }

    static void onHostileKill(EntityPlayerMP player) {
        if (LastStandData.timer(player) > 0) {
            escape(player, true);
        }
    }

    /** 玩家真的死了（/kill、虚空或处决）：清掉进行中的状态，冷却保留。 */
    static void onDeath(EntityPlayerMP player) {
        NBTTagCompound tag = LastStandData.peek(player);
        if (tag != null) {
            tag.removeTag(LastStandData.TIMER);
            tag.removeTag(LastStandData.DOOM);
            LastStandData.removeIfEmpty(player);
        }
    }

    static void syncToClient(EntityPlayerMP player) {
        EAddonsMod.PACKET_HANDLER.sendTo(new LastStandSyncMessage(LastStandData.readyAt(player)), player);
    }

    /** 生命比例：装有 First Aid 时取致命部位中最低的一处，否则用原版生命。 */
    static float healthRatio(EntityPlayer player) {
        Float firstAid = FirstAidHealth.lowestCriticalRatio(player);
        if (firstAid != null) {
            return firstAid;
        }
        float max = player.getMaxHealth();
        return max > 0.0F ? player.getHealth() / max : 0.0F;
    }

    private static boolean canTrigger(EntityPlayerMP player, DamageSource source) {
        if (!LastStandConfig.enabled || source.canHarmInCreative() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (LastStandConfig.hardcoreOnly && !player.world.getWorldInfo().isHardcoreModeEnabled()) {
            return false;
        }
        if (BaublesApi.isBaubleEquipped(player, ItemInsigniaOfDespair.INSTANCE) < 0
                || player.world.getTotalWorldTime() < LastStandData.readyAt(player)) {
            return false;
        }
        return !LastStandYield.anyWillSave(player);
    }

    private static void begin(EntityPlayerMP player) {
        player.setHealth(1.0F);
        long readyAt = player.world.getTotalWorldTime() + LastStandConfig.cooldownTicks;
        NBTTagCompound tag = LastStandData.getOrCreate(player);
        tag.setInteger(LastStandData.TIMER, LastStandConfig.durationTicks);
        tag.setLong(LastStandData.READY_AT, readyAt);
        if (LastStandConfig.speedLevel > 0) {
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, LastStandConfig.durationTicks,
                    LastStandConfig.speedLevel - 1, false, true));
        }
        showTitle(player, "message.eaddons.last_stand.title", "message.eaddons.last_stand.subtitle",
                LastStandConfig.durationTicks / 20, LastStandConfig.surviveHealthPercent);
        playEffect(player, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, EnumParticleTypes.SMOKE_LARGE);
        EAddonsMod.PACKET_HANDLER.sendTo(new LastStandSyncMessage(readyAt), player);
    }

    private static void conclude(EntityPlayerMP player, NBTTagCompound tag) {
        tag.removeTag(LastStandData.TIMER);
        if (player.isCreative() || player.isSpectator()
                || healthRatio(player) * 100.0F >= LastStandConfig.surviveHealthPercent) {
            escape(player, false);
        } else {
            tag.setInteger(LastStandData.DOOM, 1);
            doom(player, tag);
        }
    }

    private static void escape(EntityPlayerMP player, boolean byKill) {
        NBTTagCompound tag = LastStandData.peek(player);
        if (tag != null) {
            tag.removeTag(LastStandData.TIMER);
            LastStandData.removeIfEmpty(player);
        }
        if (byKill && LastStandConfig.killHealPercent > 0) {
            player.heal(player.getMaxHealth() * LastStandConfig.killHealPercent / 100.0F);
        }
        showTitle(player, "message.eaddons.last_stand.escaped_title",
                byKill ? "message.eaddons.last_stand.escaped_kill" : "message.eaddons.last_stand.escaped_health");
        playEffect(player, SoundEvents.ENTITY_PLAYER_LEVELUP, EnumParticleTypes.TOTEM);
    }

    /** 失败处决。被挡下就留到下 tick 再来，超过上限才放弃。 */
    private static void doom(EntityPlayerMP player, NBTTagCompound tag) {
        int attempt = tag.getInteger(LastStandData.DOOM);
        player.attackEntityFrom(DESPAIR, Float.MAX_VALUE);
        if (player.getHealth() > 0.0F) {
            if (attempt < DOOM_ATTEMPTS) {
                tag.setInteger(LastStandData.DOOM, attempt + 1);
                return;
            }
            LOG.warn("[LastStand] {} kept surviving the despair death, something else keeps preventing it",
                    player.getName());
        }
        tag.removeTag(LastStandData.DOOM);
        LastStandData.removeIfEmpty(player);
    }

    private static void sendCountdown(EntityPlayerMP player, int timer) {
        int seconds = (timer + 19) / 20;
        int percent = (int) (healthRatio(player) * 100.0F);
        player.sendStatusMessage(new TextComponentTranslation("message.eaddons.last_stand.countdown",
                seconds, percent, LastStandConfig.surviveHealthPercent), true);
    }

    private static void showTitle(EntityPlayerMP player, String titleKey, String subtitleKey, Object... args) {
        if (player.connection == null) {
            return;
        }
        player.connection.sendPacket(new SPacketTitle(5, 50, 10));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE,
                new TextComponentTranslation(subtitleKey, args)));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE,
                new TextComponentTranslation(titleKey)));
    }

    private static void playEffect(EntityPlayerMP player, SoundEvent sound, EnumParticleTypes particle) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS,
                1.0F, 1.0F);
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(particle, player.posX, player.posY + player.height * 0.5D,
                    player.posZ, 40, 0.4D, 0.8D, 0.4D, 0.05D);
        }
    }
}
