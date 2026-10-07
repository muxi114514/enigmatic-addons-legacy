package net.mx.eaddons.item;

import net.minecraftforge.common.config.Configuration;

/**
 * 术质共鸣者配置。默认值取自 EL+ 1.21.1，改动过的三处（烈焰满级、海洋整条、星云距离）
 * 按本次定稿填写。
 */
public class SpellstoneSwordConfig {
    private static final String CATEGORY = "SpellstoneSword";

    /** 剑的基础攻击加成，各形态在此之上叠加。 */
    public static double baseDamage = 6.0D;
    /** 耐久。 */
    public static int durability = 999;
    /** 共鸣建立/解除的二次确认窗口（tick）：第一次右键后多久内再按一次才算数。 */
    public static int resonateTime = 60;

    // ── 烈焰之核 ──
    /** 喷火每 5 tick 消耗的能量。 */
    public static int blazingFlameCost = 3;
    /** 岩浆爆消耗的能量。 */
    public static int blazingBurstCost = 10;

    // ── 海洋意志（本次重做）──
    /** 命中积攒的能量。 */
    public static int oceanEnergyPerHit = 4;
    /** 落雷伤害 = 佩戴者攻击力 × 该倍率。 */
    public static double oceanLightningRatio = 4.5D;
    /** 被动回能：每多少 tick 回一次。 */
    public static int oceanRegenInterval = 4;
    /** 被动每次回复的能量。 */
    public static int oceanRegenAmount = 2;
    /** 满级是否让佩戴者恒为「湿润」状态（被动全天候 + 冰与火潮卫套常驻）。 */
    public static boolean oceanMaxLevelAlwaysWet = true;

    // ── 失落引擎 ──
    /** 共鸣期间免疫摔落与撞墙动能伤害。抓钩的甩飞与下坠否则会反噬自己。 */
    public static boolean engineNullKinetic = true;

    // ── 星云之眼 ──
    /** 攻击距离加成（挂在 generic.reachDistance 上，对方块交互与实体攻击同时生效）。 */
    public static double nebulaReachBonus = 1.0D;

    // ── 忘却冰晶 ──
    /** 命中施加的冻结基数，实际 = 该值 + 10 × 共鸣等级。 */
    public static int frostPerHit = 40;
    /** 忘却形态满级：命中时对目标周围生物施加的冻结时长（tick）。 */
    public static int frostAoeTime = 120;
    /** 忘却形态满级的 AOE 冻结半径。 */
    public static double frostAoeRadius = 4.0D;

    // ── 原初共鸣（链接式，佩戴原初立方术石时生效）──
    /** 纯原初命中施加的灵液腐蚀时长（tick）。 */
    public static int primevalCorrosionTime = 200;
    /** 灵液腐蚀等级（1 = I 级）。 */
    public static int primevalCorrosionLevel = 2;
    /** 书袋里有恩惠之典时的腐蚀等级。 */
    public static int primevalCorrosionBlessLevel = 3;
    /** 命中后把目标的受击无敌帧压到该值（原版 20，前 10 tick 内只结算伤害差额）。 */
    public static int primevalHurtResistant = 15;
    /** 全形态技能冷却系数。 */
    public static double primevalCooldownFactor = 0.75D;

    // ── 非欧共鸣（链接式，佩戴非欧立方时生效）──
    /** 随机伤害倍率的下限与上限。 */
    public static double cubeDamageMin = 0.5D;
    public static double cubeDamageMax = 2.5D;
    /** 每点幸运把下限抬高多少，以及下限的封顶。 */
    public static double cubeLuckFloorStep = 0.1D;
    public static double cubeLuckFloorCap = 1.5D;
    /** 目标每个负面效果的增伤与其封顶。 */
    public static double cubeDebuffBonus = 0.1D;
    public static double cubeDebuffBonusCap = 0.5D;
    /** 命中时削去目标一半当前生命的概率，以及对 Boss 的伤害封顶（攻击力倍率）。 */
    public static double cubeHalfChance = 0.05D;
    public static double cubeBossCapRatio = 5.0D;
    /** 非欧领域半径：范围内非玩家生物无法瞬移。 */
    public static double cubeDomainRadius = 25.0D;
    /** 折跃的最远距离与冷却（原版冷却）。 */
    public static double cubeBlinkDistance = 12.0D;
    public static int cubeBlinkCooldown = 60;
    /** 效果对调的冷却（自建计时，不挡住折跃与解除链接）。 */
    public static int cubeSwapCooldown = 600;

    public static void init(Configuration config) {
        baseDamage = config.getFloat("BaseDamage", CATEGORY, 6.0F, 0F, 100F,
                "Base attack damage bonus of the Resonator of Spell, before per-form bonuses.");

        durability = config.getInt("Durability", CATEGORY, 999, 1, 32768,
                "Durability of the Resonator of Spell.");

        resonateTime = config.getInt("ResonateTime", CATEGORY, 60, 1, 600,
                "Confirmation window to bind or release a spellstone: ticks allowed between the two right-clicks.");

        blazingFlameCost = config.getInt("BlazingFlameCost", CATEGORY, 3, 0, 100,
                "Energy consumed every 5 ticks while breathing fire in Blazing Core form.");

        blazingBurstCost = config.getInt("BlazingBurstCost", CATEGORY, 10, 0, 100,
                "Energy consumed by the lava burst in Blazing Core form.");

        oceanEnergyPerHit = config.getInt("OceanEnergyPerHit", CATEGORY, 4, 0, 64,
                "Energy gained per hit in Ocean Stone form.");

        oceanLightningRatio = config.getFloat("OceanLightningRatio", CATEGORY, 4.5F, 0F, 100F,
                "Lightning damage in Ocean Stone form, as a multiplier of the wielder's attack damage.");

        oceanRegenInterval = config.getInt("OceanRegenInterval", CATEGORY, 4, 1, 200,
                "Ticks between passive energy regeneration in Ocean Stone form.");

        oceanRegenAmount = config.getInt("OceanRegenAmount", CATEGORY, 2, 0, 64,
                "Energy restored per passive tick in Ocean Stone form.");

        oceanMaxLevelAlwaysWet = config.getBoolean("OceanMaxLevelAlwaysWet", CATEGORY, true,
                "At max resonance, Ocean Stone form keeps the wielder permanently 'wet': "
                        + "passive always active and Ice and Fire's sea serpent armor set bonus stays on. "
                        + "Side effect: the wielder can never catch fire.");

        engineNullKinetic = config.getBoolean("EngineNullKinetic", CATEGORY, true,
                "While resonating with the Lost Engine, the wielder is immune to fall damage and "
                        + "flying-into-wall (kinetic) damage, so the grappling hook cannot kill its user.");

        nebulaReachBonus = config.getFloat("NebulaReachBonus", CATEGORY, 1.0F, 0F, 16F,
                "Reach distance bonus in Eye of the Nebula form. Applies to both block and entity reach.");

        frostPerHit = config.getInt("FrostPerHit", CATEGORY, 40, 0, 1200,
                "Base frost ticks applied per hit in Forgotten Ice form. Actual = this + 10 x resonance level.");

        frostAoeTime = config.getInt("FrostAoeTime", CATEGORY, 120, 0, 1200,
                "At max resonance, Forgotten Ice form freezes everything around the target for this many ticks.");

        frostAoeRadius = config.getFloat("FrostAoeRadius", CATEGORY, 4.0F, 0F, 32F,
                "Radius of that max-level freeze burst.");

        primevalCorrosionTime = config.getInt("PrimevalCorrosionTime", CATEGORY, 200, 0, 12000,
                "Ichor Corrosion duration applied by the pure Primeval form, in ticks.");

        primevalCorrosionLevel = config.getInt("PrimevalCorrosionLevel", CATEGORY, 2, 1, 10,
                "Ichor Corrosion level applied by the pure Primeval form.");

        primevalCorrosionBlessLevel = config.getInt("PrimevalCorrosionBlessLevel", CATEGORY, 3, 1, 10,
                "Ichor Corrosion level when The Bless is inside the player's Antique Bag.");

        primevalHurtResistant = config.getInt("PrimevalHurtResistant", CATEGORY, 15, 0, 20,
                "Primeval resonance shortens the target's hurt resistant time to this many ticks "
                        + "(vanilla is 20; within the first 10 only the damage difference is applied).");

        primevalCooldownFactor = config.getFloat("PrimevalCooldownFactor", CATEGORY, 0.75F, 0.05F, 1F,
                "Cooldown multiplier for every form's active while resonating with the Primeval Cube.");

        cubeDamageMin = config.getFloat("CubeDamageMin", CATEGORY, 0.5F, 0F, 10F,
                "Lower bound of the random damage multiplier in Non-Euclidean form.");

        cubeDamageMax = config.getFloat("CubeDamageMax", CATEGORY, 2.5F, 0F, 20F,
                "Upper bound of the random damage multiplier in Non-Euclidean form.");

        cubeLuckFloorStep = config.getFloat("CubeLuckFloorStep", CATEGORY, 0.1F, 0F, 2F,
                "How much each point of luck raises the lower bound.");

        cubeLuckFloorCap = config.getFloat("CubeLuckFloorCap", CATEGORY, 1.5F, 0F, 20F,
                "Maximum lower bound reachable through luck.");

        cubeDebuffBonus = config.getFloat("CubeDebuffBonus", CATEGORY, 0.1F, 0F, 5F,
                "Damage bonus per negative effect on the target in Non-Euclidean form.");

        cubeDebuffBonusCap = config.getFloat("CubeDebuffBonusCap", CATEGORY, 0.5F, 0F, 10F,
                "Cap of that bonus.");

        cubeHalfChance = config.getFloat("CubeHalfChance", CATEGORY, 0.05F, 0F, 1F,
                "Chance for a hit to strip half of the target's current health.");

        cubeBossCapRatio = config.getFloat("CubeBossCapRatio", CATEGORY, 5.0F, 0F, 100F,
                "Damage cap of that proc against bosses, as a multiplier of the wielder's attack damage.");

        cubeDomainRadius = config.getFloat("CubeDomainRadius", CATEGORY, 25.0F, 0F, 128F,
                "Radius in which non-player creatures cannot teleport while the wielder holds "
                        + "a sword resonating with the Non-Euclidean Cube.");

        cubeBlinkDistance = config.getFloat("CubeBlinkDistance", CATEGORY, 12.0F, 0F, 64F,
                "Maximum blink distance of the Non-Euclidean form's active.");

        cubeBlinkCooldown = config.getInt("CubeBlinkCooldown", CATEGORY, 60, 0, 12000,
                "Blink cooldown in ticks (vanilla cooldown).");

        cubeSwapCooldown = config.getInt("CubeSwapCooldown", CATEGORY, 600, 0, 24000,
                "Cooldown of the effect swap in ticks (tracked on the sword itself).");
    }
}
