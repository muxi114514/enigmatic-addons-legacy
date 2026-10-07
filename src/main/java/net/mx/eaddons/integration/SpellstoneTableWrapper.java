package net.mx.eaddons.integration;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ItemSpellstoneDebris;
import net.mx.eaddons.table.SpellstoneTableRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 一条术石工作台配方在 JEI 里的数据：残片（带所需数量）、术核、七种材料 → 术石。 */
public class SpellstoneTableWrapper implements IRecipeWrapper {

    private final SpellstoneTableRecipe recipe;

    public SpellstoneTableWrapper(SpellstoneTableRecipe recipe) {
        this.recipe = recipe;
    }

    public SpellstoneTableRecipe getRecipe() {
        return this.recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        // 每一格都是一组候选：矿物词典材料会在 JEI 里轮播展示
        List<List<ItemStack>> inputs = new ArrayList<>();
        inputs.add(Collections.singletonList(
                new ItemStack(ItemSpellstoneDebris.INSTANCE, this.recipe.getDebrisCount())));
        // 核心槽按配方来：普通配方是术质核心，原初立方的内置配方各有自己的核心
        inputs.add(Arrays.asList(this.recipe.getCore().getMatchingStacks()));
        for (net.minecraft.item.crafting.Ingredient ingredient : this.recipe.getIngredients()) {
            inputs.add(Arrays.asList(ingredient.getMatchingStacks()));
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getResult());
    }
}
