package net.mx.eaddons.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.primeval.ItemPrimevalCube;
import net.mx.eaddons.table.BlockSpellstoneTable;
import net.mx.eaddons.table.SpellstoneTableRecipe;
import net.mx.eaddons.table.SpellstoneTableRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 集成：把术石工作台的配方展示出来，并把工作台方块登记为该分类的操作台。
 *
 * <p>带 {@code @JEIPlugin} 的类只有 JEI 自己在运行时才会去扫描并实例化，
 * 所以 JEI 缺席时这个类根本不会被加载，不需要额外的门控。
 */
@JEIPlugin
public class EAddonsJEIPlugin implements IModPlugin {

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(
                new SpellstoneTableCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        List<SpellstoneTableWrapper> wrappers = new ArrayList<>();
        for (SpellstoneTableRecipe recipe : SpellstoneTableRecipes.all()) {
            wrappers.add(new SpellstoneTableWrapper(recipe));
        }
        registry.addRecipes(wrappers, SpellstoneTableCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(BlockSpellstoneTable.ITEM_BLOCK), SpellstoneTableCategory.UID);
        // 原初立方：没有合成配方，获取与铭刻方式写在说明页里
        registry.addIngredientInfo(new ItemStack(ItemPrimevalCube.INSTANCE), VanillaTypes.ITEM,
                "jei.eaddons.primeval_cube.info1", "jei.eaddons.primeval_cube.info2", "jei.eaddons.primeval_cube.info3");
    }
}
