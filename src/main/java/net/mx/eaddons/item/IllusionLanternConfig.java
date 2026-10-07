package net.mx.eaddons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 幻影灵魂灯笼配置。移植自 1.20 神遗拓展 IllusionLantern 的 onConfig。
 */
public class IllusionLanternConfig {
    private static final String CATEGORY = "IllusionLantern";
    /** 巧克力任务的怪物（含 Boss 连战刷出的）默认不吃冥灯的亡灵中立 */
    private static final String[] DEFAULT_TRUCE_BLACKLIST = {"chocolatequest:*", "chocotweak:*"};

    /** 亡灵中立名单：完整实体 id 与整模组（modid:*），冥灯本体与调谐器冥灯被动共用；不可变集合，读写跨线程安全。 */
    private static volatile Set<String> truceBlacklistIds = Collections.emptySet();
    private static volatile Set<String> truceBlacklistMods = Collections.emptySet();

    /** 生成一颗灵魂火球的冷却（tick）。 */
    public static int fireballCooldown = 60;
    /** 受到非魔法伤害的倍率百分比：1.20 asModifier(true)=1+p/100，故为易伤。 */
    public static int nonMagicDamagePercent = 25;
    /** 受到穿甲类伤害的减免百分比：最终乘以 (1 - p/100)。 */
    public static int bypassDamageResistancePercent = 50;

    public static void init(Configuration config) {
        fireballCooldown = config.getInt("FireballCooldown", CATEGORY, 60, 1, 32768,
                "Cooldown (ticks) of soul fireball generation.");

        nonMagicDamagePercent = config.getInt("NonMagicDamagePercent", CATEGORY, 25, 0, 1000,
                "Extra non-magic damage taken, in percent (25 = take 25% more).");

        bypassDamageResistancePercent = config.getInt("BypassDamageResistancePercent", CATEGORY, 50, 0, 80,
                "Resistance to defence-bypassing damage, in percent (50 = take 50% less).");

        String[] truce = config.getStringList("UndeadTruceBlackList", CATEGORY, DEFAULT_TRUCE_BLACKLIST,
                "Undead that still target the wearer of the Illusion Lantern, and of a Spelltuner tuned to it. "
                        + "Format: modid:entityname, or modid:* for every entity of a mod.");
        Set<String> ids = new HashSet<>();
        Set<String> mods = new HashSet<>();
        for (String entry : truce) {
            String id = entry == null ? "" : entry.trim().toLowerCase(Locale.ROOT);
            if (id.endsWith(":*")) {
                mods.add(id.substring(0, id.length() - 2));
            } else if (!id.isEmpty()) {
                ids.add(new ResourceLocation(id).toString());
            }
        }
        truceBlacklistIds = Collections.unmodifiableSet(ids);
        truceBlacklistMods = Collections.unmodifiableSet(mods);
    }

    /** 该实体在亡灵中立名单里：照常把冥灯佩戴者当目标。 */
    public static boolean isTruceBlacklisted(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        return id != null && (truceBlacklistMods.contains(id.getResourceDomain())
                || truceBlacklistIds.contains(id.toString()));
    }

    /** 非魔法伤害倍率（>1 即易伤）。 */
    public static float getNonMagicMultiplier() {
        return 1.0F + nonMagicDamagePercent / 100.0F;
    }

    /** 穿甲伤害倍率（<1 即减免）。 */
    public static float getBypassMultiplier() {
        return 1.0F - bypassDamageResistancePercent / 100.0F;
    }
}
