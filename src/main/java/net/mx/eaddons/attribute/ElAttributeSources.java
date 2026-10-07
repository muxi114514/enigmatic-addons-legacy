package net.mx.eaddons.attribute;

import static net.mx.eaddons.attribute.EnigmaticAttributes.*;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.EnigmaticConfigs;
import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import keletu.enigmaticlegacy.item.etherium.EtheriumArmor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.potion.PotionEffect;

/**
 * EL 1.12 原本写死在 EnigmaticEvents 里的效果，改为属性来源。
 * <p>数值全部实时读 EL 自己的配置，整合包改 enigmaticlegacy.cfg 照样生效；
 * EL 那边对应的旧逻辑由 {@code MixinEnigmaticEventsAttributes} 停用，避免重复结算。
 * 七色护符的判定沿用 {@code isWearEnigmaticAmulet}，飞升护符视为佩戴全部颜色。
 */
final class ElAttributeSources {

    /** 饰品 / 手持类来源的求值间隔 */
    private static final int INTERVAL = 5;

    private ElAttributeSources() {
    }

    static void register() {
        // 吸血
        AttributeSources.register("el.amulet_black", LIFESTEAL, 0, INTERVAL,
                p -> amulet(p, 6) ? EnigmaticConfigs.enigmaticAmuletLifesteal : 0);
        AttributeSources.register("el.eldritch_amulet", LIFESTEAL, 0, INTERVAL,
                p -> equipped(p, EnigmaticLegacy.eldritchAmulet) && SuperpositionHandler.isTheWorthyOne(p)
                        ? EnigmaticConfigs.eldritchAmuletLifesteal : 0);
        // 无止之言：原代码写死 0.1，配置项 LifestealBonus 从未被读取，这里改为读配置
        AttributeSources.register("el.the_infinitum", LIFESTEAL, 0, 1,
                p -> mainHand(p, EnigmaticLegacy.theInfinitum) && SuperpositionHandler.isTheWorthyOne(p)
                        ? EnigmaticConfigs.infinitumLifestealBonus : 0);
        AttributeSources.register("el.eldritch_pan", LIFESTEAL, 0, 1,
                p -> mainHand(p, EnigmaticLegacy.eldritchPan) && SuperpositionHandler.isTheWorthyOne(p)
                        ? EnigmaticConfigs.panLifeSteal : 0);
        AttributeSources.register("el.growing_bloodlust", LIFESTEAL, 0, INTERVAL, p -> {
            PotionEffect effect = p.getActivePotionEffect(EnigmaticLegacy.growingBloodlust);
            return effect == null ? 0 : EnigmaticConfigs.bloodLustLifestealBoost * (effect.getAmplifier() + 1);
        });

        // 投射物偏转
        AttributeSources.register("el.angel_blessing", PROJECTILE_DEFLECT, 0, INTERVAL,
                p -> equipped(p, EnigmaticLegacy.angelBlessing) ? EnigmaticConfigs.angelBlessingDeflectChance : 0);
        AttributeSources.register("el.the_cube_deflect", PROJECTILE_DEFLECT, 0, INTERVAL,
                p -> equipped(p, EnigmaticLegacy.the_cube) ? EnigmaticConfigs.cubeDeflectionChance : 0);
        AttributeSources.register("el.amulet_violet", PROJECTILE_DEFLECT, 0, INTERVAL,
                p -> amulet(p, 3) ? EnigmaticConfigs.enigmaticAmuletDeflectionChance : 0);

        // 挖掘加速
        AttributeSources.register("el.mining_charm", MINING_SPEED, 0, INTERVAL,
                p -> equipped(p, EnigmaticLegacy.miningCharm) ? EnigmaticConfigs.breakSpeedBonus : 0);
        // 诅咒数要遍历身上物品，算得慢一些
        AttributeSources.register("el.cursed_scroll", MINING_SPEED, 0, 20,
                p -> equipped(p, EnigmaticLegacy.cursedScroll)
                        ? EnigmaticConfigs.cursedScrollMiningBoost * SuperpositionHandler.getCurseAmount(p) : 0);
        AttributeSources.register("el.amulet_green", MINING_SPEED, 0, INTERVAL,
                p -> amulet(p, 5) ? EnigmaticConfigs.enigmaticAmuletMiningSpeedBonus : 0);
        AttributeSources.register("el.the_cube_mining", MINING_SPEED, 0, INTERVAL,
                p -> equipped(p, EnigmaticLegacy.the_cube) ? EnigmaticConfigs.cubeMiningSpeedBonus : 0);

        // 品红护符：缓降、跳跃、小额摔伤免疫（乘总值，与原先逐个相乘等价）
        AttributeSources.register("el.amulet_magenta_fall", FALL_SPEED, 2, INTERVAL,
                p -> amulet(p, 4) ? EnigmaticConfigs.enigmaticAmuletSlowFallMultiplier - 1 : 0);
        AttributeSources.register("el.amulet_magenta_jump", JUMP_BOOST, 2, INTERVAL,
                p -> amulet(p, 4) ? EnigmaticConfigs.enigmaticAmuletJumpMultiplier - 1 : 0);
        AttributeSources.register("el.amulet_magenta_fall_immunity", FALL_IMMUNITY, 0, INTERVAL,
                p -> amulet(p, 4) ? EnigmaticConfigs.enigmaticAmuletFallDamageImmunityThreshold : 0);

        // 失落引擎（附加物品，仅在 AllowAddonItems 时注册）
        AttributeSources.register("el.lost_engine_fall", FALL_SPEED, 2, INTERVAL,
                p -> lostEngine(p) ? Math.max(EnigmaticConfigs.lostEngineGravityModifier / 10, 0) : 0);
        AttributeSources.register("el.lost_engine_crit", CRIT_DAMAGE, 0, INTERVAL,
                p -> lostEngine(p) ? EnigmaticConfigs.lostEngineCritModifier : 0);

        // 以太套装：原先写死「满套且生命 ≤ 40%」
        AttributeSources.register("el.etherium_set", ETHERIUM_SHIELD, 0, INTERVAL,
                p -> EtheriumArmor.hasFullSet(p) ? 0.4 : 0);
    }

    private static boolean amulet(EntityPlayer player, int color) {
        return SuperpositionHandler.isWearEnigmaticAmulet(player, color);
    }

    private static boolean equipped(EntityPlayer player, Item item) {
        return BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    private static boolean mainHand(EntityPlayer player, Item item) {
        return player.getHeldItemMainhand().getItem() == item;
    }

    private static boolean lostEngine(EntityPlayer player) {
        return EnigmaticConfigs.allowAddonItems && equipped(player, EnigmaticLegacy.lostEngine);
    }
}
