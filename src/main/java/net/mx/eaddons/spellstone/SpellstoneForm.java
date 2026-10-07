package net.mx.eaddons.spellstone;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 术质共鸣者的「共鸣形态」适配表：术石物品 → 形态序号 / 颜色 / 能量上限公式。
 *
 * <p>EL+ 原版靠 {@code instanceof 自家 SpellstoneItem} 判形态，本移植改为按术石【注册名】映射，
 * 直接复用前置里已有的术石。1.12.2 这边十颗术石全部落在 {@code enigmaticlegacy} 域
 * （EL 的 {@code ItemBase} 构造里硬编码了该命名空间，eaddons 的术石继承它，域也随之），
 * 比 1.20.1 跨两个命名空间更简单。
 *
 * <p>序号对应 EL+ 的 {@code SpellstoneSword.Form}，模型 override 用 index/10 取值。
 */
public enum SpellstoneForm {
    NONE(0, "", 0, 0, 0),
    GOLEM_HEART(1, "golem_heart", 0xFFE40B0B, 0, 0),
    /** 1.20 叫 blazing_core，1.12.2 的注册名是 magma_heart（中文同为「烈焰之核」）。 */
    BLAZING_CORE(2, "magma_heart", 0xFFD75E12, 100, 0),
    OCEAN_STONE(3, "ocean_stone", 0xFF35ACF7, 64, 0),
    FORGOTTEN_ICE(4, "forgotten_ice", 0xFF80E5FF, 100, 0),
    REVIVAL_LEAF(5, "revival_leaf", 0xFF91D93F, 32, 0),
    ANGEL_BLESSING(6, "angel_blessing", 0xFFB2DAFF, 7, 1),
    LOST_ENGINE(7, "lost_engine", 0xFFEF9F4D, 125, 0),
    ILLUSION_LANTERN(8, "illusion_lantern", 0xE86DD5DE, 15, 3),
    EYE_OF_NEBULA(9, "eye_of_nebula", 0xFF0BDDB8, 0, 0),
    VOID_PEARL(10, "void_pearl", 0xFF333333, 3, 1),
    /** 原初立方术石：纯原初形态，也是十形态轮换的起点。术石不被吃入，只建立链接。 */
    PRIMEVAL_CUBE(11, "primeval_spellstone", 0xFFFFC61A, 0, 0),
    /** 非欧立方，同样只建立链接。 */
    THE_CUBE(12, "the_cube", 0xFF7EAFFC, 0, 0);

    /** 十颗术石的共同命名空间。 */
    public static final String NAMESPACE = "enigmaticlegacy";

    private static final SpellstoneForm[] VALUES = values();

    private final int index;
    private final String path;
    private final ResourceLocation itemId;
    private final int color;
    private final int energyBase;
    private final int energyPerLevel;

    SpellstoneForm(int index, String path, int color, int energyBase, int energyPerLevel) {
        this.index = index;
        this.path = path;
        this.itemId = path.isEmpty() ? null : new ResourceLocation(NAMESPACE, path);
        this.color = color;
        this.energyBase = energyBase;
        this.energyPerLevel = energyPerLevel;
    }

    public int index() {
        return this.index;
    }

    public int color() {
        return this.color;
    }

    /** 术石注册名的 path，同时用作模型子路径与 tooltip 键后缀。 */
    public String path() {
        return this.path;
    }

    public ResourceLocation itemId() {
        return this.itemId;
    }

    /** 该形态在给定共鸣等级下的能量上限，0 表示该形态不使用能量。 */
    public int maxEnergy(int level) {
        return this.energyBase + this.energyPerLevel * level;
    }

    /** 该术石对应的形态；非共鸣术石返回 {@link #NONE}。 */
    public static SpellstoneForm byItem(Item item) {
        if (item == null) {
            return NONE;
        }
        ResourceLocation id = item.getRegistryName();
        if (id == null) {
            return NONE;
        }
        for (SpellstoneForm form : VALUES) {
            if (id.equals(form.itemId)) {
                return form;
            }
        }
        return NONE;
    }

    public static SpellstoneForm byStack(ItemStack stack) {
        return stack.isEmpty() ? NONE : byItem(stack.getItem());
    }

    public static SpellstoneForm byIndex(int index) {
        for (SpellstoneForm form : VALUES) {
            if (form.index == index) {
                return form;
            }
        }
        return NONE;
    }

    /** 是否为可共鸣术石（能被剑吃入变形态）。两颗立方走链接、不被吃入，所以排除在外。 */
    public static boolean isResonatable(ItemStack stack) {
        SpellstoneForm form = byStack(stack);
        return form != NONE && !form.isLink();
    }

    /** 两颗立方：共鸣只记形态、不收走术石，且要求玩家佩戴着它才生效。 */
    public boolean isLink() {
        return this == PRIMEVAL_CUBE || this == THE_CUBE;
    }

    /** 副手这颗术石是否走链接式共鸣。 */
    public static SpellstoneForm linkFormOf(ItemStack stack) {
        SpellstoneForm form = byStack(stack);
        return form.isLink() ? form : NONE;
    }

    /** 十形态切换顺序：纯原初 → 魔像 … → 虚空 → 纯原初。 */
    public static SpellstoneForm cycle(SpellstoneForm current, boolean backward) {
        int span = VOID_PEARL.index;   // 1~10 号加上纯原初，共 11 个状态
        int now = current == PRIMEVAL_CUBE ? 0 : current.index;
        int next = (now + (backward ? span : 1)) % (span + 1);
        return next == 0 ? PRIMEVAL_CUBE : byIndex(next);
    }
}
