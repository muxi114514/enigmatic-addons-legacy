package net.mx.eaddons.table;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 术石工作台的一条配方：核心（默认术核）+ 若干残片 + 最多 7 种材料 → 产物。
 *
 * <p>材料**无序匹配**，与 1.20.1 一致。1.12.2 没有数据包驱动的 {@code RecipeSerializer}，
 * 故改成代码里维护的配方表，见 {@link SpellstoneTableRecipes}。
 *
 * <p>材料类型用 Forge 的 {@link Ingredient} 而不是裸 {@link ItemStack}，这样矿物词典、
 * 多候选、NBT 匹配都能表达，CraftTweaker 的 {@code IIngredient} 也能直接转过来。
 */
public class SpellstoneTableRecipe {

    public static final int SLOT_DEBRIS = 0;
    public static final int SLOT_CORE = 1;
    public static final int SLOT_SPELLSTONE = 2;
    public static final int SLOT_INGREDIENT_START = 3;
    public static final int INGREDIENT_COUNT = 7;
    /** 输入容器大小：残片 + 术核 + 术石 + 7 材料。 */
    public static final int CRAFT_SIZE = SLOT_INGREDIENT_START + INGREDIENT_COUNT;

    private final ItemStack result;
    private final int debrisCount;
    private final List<Ingredient> ingredients;
    private final boolean allDifferent;
    /** 核心槽要放什么。普通配方是术质核心，原初立方那两条内置配方各有自己的核心。 */
    private final Ingredient core;

    public SpellstoneTableRecipe(ItemStack result, int debrisCount, List<Ingredient> ingredients, boolean allDifferent) {
        this(result, debrisCount, ingredients, allDifferent, Ingredient.fromItem(net.mx.eaddons.item.ItemSpellcore.INSTANCE));
    }

    public SpellstoneTableRecipe(ItemStack result, int debrisCount, List<Ingredient> ingredients, boolean allDifferent,
                                 Ingredient core) {
        this.result = result;
        this.debrisCount = debrisCount;
        this.ingredients = ingredients;
        this.allDifferent = allDifferent;
        this.core = core;
    }

    public Ingredient getCore() {
        return this.core;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public int getDebrisCount() {
        return this.debrisCount;
    }

    public List<Ingredient> getIngredients() {
        return this.ingredients;
    }

    /** 该配方能否用当前输入合成。 */
    public boolean matches(IInventory craft) {
        if (!this.core.apply(craft.getStackInSlot(SLOT_CORE))) {
            return false;
        }
        if (!craft.getStackInSlot(SLOT_SPELLSTONE).isEmpty()) {
            return false;   // 术石槽有东西时是回收模式，不走合成
        }
        if (craft.getStackInSlot(SLOT_DEBRIS).getCount() < this.debrisCount) {
            return false;
        }

        List<ItemStack> given = new ArrayList<>();
        for (int i = 0; i < INGREDIENT_COUNT; i++) {
            ItemStack stack = craft.getStackInSlot(SLOT_INGREDIENT_START + i);
            if (!stack.isEmpty()) {
                given.add(stack);
            }
        }
        if (given.size() != this.ingredients.size()) {
            return false;
        }
        if (this.allDifferent) {
            Set<String> seen = new HashSet<>();
            for (ItemStack stack : given) {
                if (!seen.add(keyOf(stack))) {
                    return false;
                }
            }
        }
        return matchUnordered(given);
    }

    /** 无序匹配：每个需求各认领一个尚未被用掉的输入。 */
    private boolean matchUnordered(List<ItemStack> given) {
        boolean[] used = new boolean[given.size()];
        for (Ingredient required : this.ingredients) {
            boolean found = false;
            for (int i = 0; i < given.size(); i++) {
                if (!used[i] && required.apply(given.get(i))) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static String keyOf(ItemStack stack) {
        return stack.getItem().getRegistryName() + "@" + stack.getMetadata();
    }
}
