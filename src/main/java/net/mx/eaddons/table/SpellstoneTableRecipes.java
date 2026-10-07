package net.mx.eaddons.table;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.spellstone.SpellstoneForm;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 术石工作台的配方表。
 *
 * <p>1.20.1 用数据包 + {@code RecipeSerializer}，1.12.2 没有这套自定义配方的序列化基建，
 * 故在代码里维护。在配方注册事件里建表（见 EAddonsMod#registerRecipes）：物品已全部注册，且早于 CraftTweaker 执行脚本。
 *
 * <p>材料全部重拟过：原版配方用的紫水晶、幻翼膜、哭泣黑曜石都是 1.13 以后才有的东西，
 * 这里按同样的主题换成了 1.12.2 的等价物。
 */
public final class SpellstoneTableRecipes {

    /** 回收一颗术石返还的残片数。 */
    public static final int RECYCLE_DEBRIS = 4;

    private static final List<SpellstoneTableRecipe> RECIPES = new ArrayList<>();

    private SpellstoneTableRecipes() {
    }

    public static List<SpellstoneTableRecipe> all() {
        return RECIPES;
    }

    @Nullable
    public static SpellstoneTableRecipe find(IInventory craft) {
        for (SpellstoneTableRecipe recipe : RECIPES) {
            if (recipe.matches(craft)) {
                return recipe;
            }
        }
        return null;
    }

    /** 能不能放进核心槽：术质核心，或任何一条配方要求的核心。 */
    public static boolean isCoreItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == net.mx.eaddons.item.ItemSpellcore.INSTANCE) {
            return true;
        }
        for (SpellstoneTableRecipe recipe : RECIPES) {
            if (recipe.getCore().apply(stack)) {
                return true;
            }
        }
        return false;
    }

    public static void init() {
        RECIPES.clear();
        // 魔像之心：铁与石，主打硬
        add(SpellstoneForm.GOLEM_HEART, 8,
                "minecraft:iron_block", "minecraft:iron_block",
                "minecraft:iron_ingot", "minecraft:iron_ingot",
                "minecraft:redstone_block", "minecraft:pumpkin", "minecraft:quartz");
        // 烈焰之核：下界的火
        add(SpellstoneForm.BLAZING_CORE, 8,
                "minecraft:blaze_powder", "minecraft:blaze_powder",
                "minecraft:magma_cream", "minecraft:magma_cream",
                "minecraft:glowstone", "minecraft:fire_charge", "minecraft:quartz");
        // 海洋意志：海晶与深海
        add(SpellstoneForm.OCEAN_STONE, 8,
                "minecraft:prismarine_shard", "minecraft:prismarine_shard",
                "minecraft:prismarine_crystals", "minecraft:prismarine_crystals",
                "minecraft:sponge", "minecraft:fish:3", "minecraft:quartz");
        // 忘却冰晶：冰与雪
        add(SpellstoneForm.FORGOTTEN_ICE, 8,
                "minecraft:packed_ice", "minecraft:packed_ice",
                "minecraft:ice", "minecraft:ice",
                "minecraft:snow", "minecraft:snowball", "minecraft:quartz");
        // 复苏之叶：农作与治疗
        add(SpellstoneForm.REVIVAL_LEAF, 8,
                "minecraft:golden_apple", "minecraft:speckled_melon",
                "minecraft:wheat", "minecraft:wheat",
                "minecraft:leaves", "minecraft:leaves", "minecraft:quartz");
        // 天使之祝：飞行与光
        add(SpellstoneForm.ANGEL_BLESSING, 8,
                "minecraft:ghast_tear", "minecraft:ghast_tear",
                "minecraft:glowstone", "minecraft:glowstone",
                "minecraft:feather", "minecraft:quartz_block", "minecraft:quartz");
        // 失落引擎：机械
        add(SpellstoneForm.LOST_ENGINE, 8,
                "minecraft:piston", "minecraft:piston",
                "minecraft:redstone_block", "minecraft:redstone_block",
                "minecraft:hopper", "minecraft:iron_ingot", "minecraft:quartz");
        // 幻影灵魂灯笼：灵魂
        add(SpellstoneForm.ILLUSION_LANTERN, 8,
                "minecraft:soul_sand", "minecraft:soul_sand",
                "minecraft:nether_wart", "minecraft:nether_wart",
                "minecraft:glowstone", "minecraft:ender_eye", "minecraft:quartz");
        // 星云之眼：末地与传送
        add(SpellstoneForm.EYE_OF_NEBULA, 8,
                "minecraft:ender_pearl", "minecraft:ender_pearl",
                "minecraft:chorus_fruit", "minecraft:chorus_fruit",
                "minecraft:ender_eye", "minecraft:end_stone", "minecraft:quartz");
        // 虚空珍珠：黑曜石与虚空
        add(SpellstoneForm.VOID_PEARL, 8,
                "minecraft:obsidian", "minecraft:obsidian",
                "minecraft:ender_pearl", "minecraft:ender_pearl",
                "minecraft:coal", "minecraft:end_stone", "minecraft:quartz");
    }

    private static void add(SpellstoneForm form, int debris, String... ingredientIds) {
        if (form.itemId() == null) {
            return;
        }
        Item stone = ForgeRegistries.ITEMS.getValue(form.itemId());
        if (stone == null) {
            return;   // 该术石在当前前置组合下不存在，跳过这条配方
        }
        List<Ingredient> ingredients = new ArrayList<>();
        for (String id : ingredientIds) {
            ItemStack stack = parse(id);
            if (stack.isEmpty()) {
                return;   // 材料缺失就整条不注册，免得出现永远合不出来的配方
            }
            ingredients.add(Ingredient.fromStacks(stack));
        }
        RECIPES.add(new SpellstoneTableRecipe(new ItemStack(stone), debris, ingredients, false));
    }

    // ============================ 供 CraftTweaker 调用 ============================

    /** 追加一条配方，核心槽放术质核心。材料数量必须在 1~{@link SpellstoneTableRecipe#INGREDIENT_COUNT} 之间。 */
    public static boolean addRecipe(ItemStack result, int debris, List<Ingredient> ingredients) {
        return addRecipe(result, debris, ingredients, null);
    }

    /** 追加一条配方并指定核心槽放什么；core 为 null 时用术质核心。 */
    public static boolean addRecipe(ItemStack result, int debris, List<Ingredient> ingredients,
                                    @Nullable Ingredient core) {
        if (result.isEmpty() || ingredients.isEmpty()
                || ingredients.size() > SpellstoneTableRecipe.INGREDIENT_COUNT) {
            return false;
        }
        RECIPES.add(core == null
                ? new SpellstoneTableRecipe(result.copy(), Math.max(0, debris), ingredients, false)
                : new SpellstoneTableRecipe(result.copy(), Math.max(0, debris), ingredients, false, core));
        return true;
    }

    /** 按产出物移除配方，返回移除的条数。 */
    public static int removeRecipe(ItemStack result) {
        int before = RECIPES.size();
        RECIPES.removeIf(recipe -> ItemStack.areItemsEqual(recipe.getResult(), result));
        return before - RECIPES.size();
    }

    /** 清空全部配方。 */
    public static int removeAll() {
        int count = RECIPES.size();
        RECIPES.clear();
        return count;
    }

    /** 支持 "modid:name" 与 "modid:name:meta" 两种写法。 */
    private static ItemStack parse(String id) {
        String[] parts = id.split(":");
        int meta = 0;
        if (parts.length > 2) {
            meta = Integer.parseInt(parts[2]);
            id = parts[0] + ":" + parts[1];
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, 1, meta);
    }

    /** 供调试查看某条配方的材料。 */
    public static List<String> describe(SpellstoneTableRecipe recipe) {
        List<String> lines = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] matching = ingredient.getMatchingStacks();
            lines.add(matching.length == 0 ? "?" : matching[0].getDisplayName());
        }
        return Arrays.asList(lines.toArray(new String[0]));
    }
}
