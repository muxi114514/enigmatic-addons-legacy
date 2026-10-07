package net.mx.eaddons.despair;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.compat.ModCompat;

import javax.annotation.Nullable;
import java.util.List;

/** 客户端：缓存冷却结束时间，拼证章 tooltip 里绝境的部分。 */
@SideOnly(Side.CLIENT)
public final class LastStandClient {
    /** 网络线程写、主线程读。 */
    private static volatile long readyAt;

    private LastStandClient() {
    }

    static void setReadyAt(long value) {
        readyAt = value;
    }

    /** 简介与状态常驻，按住 Shift 展开完整规则。 */
    public static void appendTooltip(@Nullable World world, List<String> list) {
        int seconds = LastStandConfig.durationTicks / 20;
        list.add(I18n.format("tooltip.eaddons.last_stand.brief", seconds));
        list.add(statusLine(world));
        if (!GuiScreen.isShiftKeyDown()) {
            list.add(I18n.format("tooltip.eaddons.last_stand.hold_shift"));
            return;
        }
        list.add(I18n.format("tooltip.eaddons.last_stand.rule1"));
        list.add(LastStandConfig.speedLevel > 0
                ? I18n.format("tooltip.eaddons.last_stand.rule2", seconds,
                        I18n.format("enchantment.level." + LastStandConfig.speedLevel))
                : I18n.format("tooltip.eaddons.last_stand.rule2_nospeed", seconds));
        list.add(I18n.format("tooltip.eaddons.last_stand.rule3", LastStandConfig.killHealPercent));
        list.add(I18n.format("tooltip.eaddons.last_stand.rule4", LastStandConfig.surviveHealthPercent));
        list.add(I18n.format("tooltip.eaddons.last_stand.rule5"));
        list.add(I18n.format("tooltip.eaddons.last_stand.rule6", formatDuration(LastStandConfig.cooldownTicks)));
        if (ModCompat.FIRST_AID) {
            list.add(I18n.format("tooltip.eaddons.last_stand.rule_firstaid"));
        }
    }

    private static String statusLine(@Nullable World world) {
        if (!LastStandConfig.enabled) {
            return I18n.format("tooltip.eaddons.last_stand.disabled");
        }
        if (world != null && LastStandConfig.hardcoreOnly && !world.getWorldInfo().isHardcoreModeEnabled()) {
            return I18n.format("tooltip.eaddons.last_stand.inactive_world");
        }
        long remaining = world == null ? 0L : readyAt - world.getTotalWorldTime();
        if (remaining <= 0L) {
            return I18n.format("tooltip.eaddons.last_stand.ready");
        }
        long secs = (remaining + 19) / 20;
        return I18n.format("tooltip.eaddons.last_stand.cooldown", String.format("%d:%02d", secs / 60, secs % 60));
    }

    private static String formatDuration(int ticks) {
        int secs = ticks / 20;
        if (secs >= 60 && secs % 60 == 0) {
            return I18n.format("tooltip.eaddons.last_stand.time_min", secs / 60);
        }
        if (secs >= 60) {
            return I18n.format("tooltip.eaddons.last_stand.time_min_sec", secs / 60, secs % 60);
        }
        return I18n.format("tooltip.eaddons.last_stand.time_sec", secs);
    }
}
