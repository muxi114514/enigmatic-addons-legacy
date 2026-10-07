package net.mx.eaddons.attribute;

import static net.mx.eaddons.attribute.EnigmaticAttributes.*;

import keletu.enigmaticlegacy.EnigmaticConfigs;
import net.minecraft.util.ResourceLocation;
import net.mx.eaddons.item.EarthPromiseConfig;
import net.mx.eaddons.item.EtheriumCoreConfig;
import net.mx.eaddons.item.EtheriumCoreEventHandler;
import net.mx.eaddons.item.ItemAntiqueBag;
import net.mx.eaddons.item.ItemEarthPromise;
import net.mx.eaddons.item.ItemEtheriumCore;

/** 本模组自己原先写死在事件里、现改为属性的来源 */
final class EAddonsAttributeSources {

    private static final ResourceLocation THE_INFINITUM = new ResourceLocation("enigmaticlegacy", "the_infinitum");

    private EAddonsAttributeSources() {
    }

    static void register() {
        // 古旧书袋里的无止之言：吸血（原写死 0.1，改读 EL 的无止之言配置，本包同为 0.1）。
        // 查书袋要扫背包与末影箱并反序列化，间隔放宽到 2 秒
        AttributeSources.register("eaddons.antique_bag_infinitum", LIFESTEAL, 0, 40,
                p -> ItemAntiqueBag.hasItemInBag(p, THE_INFINITUM) ? EnigmaticConfigs.infinitumLifestealBonus : 0);

        // 大地之约：挖掘加速（原为对新速度整体 ×1.2，改为与其它来源相加）
        AttributeSources.register("eaddons.earth_promise", MINING_SPEED, 0, 5,
                p -> ItemEarthPromise.hasEarthPromise(p) ? EarthPromiseConfig.getBreakSpeedMultiplier() : 0);

        // 以太核心：护盾阈值 ×倍率（乘基础值，作用在以太套装等提供的阈值上）
        AttributeSources.register("eaddons.etherium_core_threshold", ETHERIUM_SHIELD, 1, 5,
                p -> ItemEtheriumCore.hasEtheriumCore(p) ? EtheriumCoreConfig.shieldThresholdMultiplier - 1 : 0);
        // 以太核心主动护盾持续期间：阈值拉满，任何血量都生效
        AttributeSources.register("eaddons.etherium_core_active", ETHERIUM_SHIELD, 0, 1,
                p -> ItemEtheriumCore.hasEtheriumCore(p) && EtheriumCoreEventHandler.getShieldTicks(p) > 0 ? 1.0 : 0);
    }
}
