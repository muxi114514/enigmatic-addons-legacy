package net.mx.eaddons.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.mx.eaddons.table.SpellstoneTableRecipe;
import net.mx.eaddons.table.SpellstoneTableRecipes;
import net.mx.eaddons.primeval.CompletePrimevalCubeIngredient;
import net.mx.eaddons.primeval.ItemPrimevalCube;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 术石工作台的 CraftTweaker 接口，让整合包用脚本增删配方而不必改 jar。
 *
 * <pre>
 * mods.eaddons.SpellstoneTable.addRecipe(&lt;enigmaticlegacy:golem_heart&gt;, 8,
 *         [&lt;minecraft:iron_block&gt;, &lt;ore:ingotIron&gt;, &lt;minecraft:pumpkin&gt;]);
 * // 第 4 个参数指定核心槽放什么（省略则为术质核心）；写原初立方时只认十种术石都铭刻完的
 * mods.eaddons.SpellstoneTable.addRecipe(&lt;enigmaticlegacy:primeval_spellstone&gt;, 32,
 *         [&lt;enigmaticlegacy:astral_dust&gt;, &lt;enigmaticlegacy:cosmic_heart&gt;], &lt;eaddons:primeval_cube&gt;);
 * mods.eaddons.SpellstoneTable.removeRecipe(&lt;enigmaticlegacy:golem_heart&gt;);
 * mods.eaddons.SpellstoneTable.removeAll();
 * </pre>
 *
 * <p>材料走 {@code CraftTweakerMC.getIngredient}，所以 {@code <ore:...>} 这类矿物词典写法直接可用。
 * 本模组在配方注册事件的默认优先级建表，CrT 在同一事件的 LOWEST 执行脚本，所以脚本的增删一定在建表之后。
 */
@ZenRegister
@ZenClass("mods.eaddons.SpellstoneTable")
public class SpellstoneTableTweaker {

    private SpellstoneTableTweaker() {
    }

    /**
     * 追加一条配方。
     *
     * @param output      产出的术石
     * @param debrisCount 消耗的术石残片数
     * @param ingredients 1~7 种材料，无序匹配
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, int debrisCount, IIngredient[] ingredients) {
        CraftTweakerAPI.apply(new Add(output, debrisCount, ingredients, null));
    }

    /**
     * 追加一条配方并指定核心槽放什么。核心写原初立方（{@code <eaddons:primeval_cube>}）时，只认十种术石都铭刻完的。
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, int debrisCount, IIngredient[] ingredients, IIngredient core) {
        CraftTweakerAPI.apply(new Add(output, debrisCount, ingredients, core));
    }

    /** 按产出的术石移除配方。 */
    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        CraftTweakerAPI.apply(new Remove(output));
    }

    /** 清空全部术石台配方。 */
    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new RemoveAll());
    }

    // ============================ 动作 ============================

    private static class Add implements IAction {
        private final IItemStack output;
        private final int debrisCount;
        private final IIngredient[] ingredients;
        @Nullable
        private final IIngredient core;

        Add(IItemStack output, int debrisCount, IIngredient[] ingredients, @Nullable IIngredient core) {
            this.output = output;
            this.debrisCount = debrisCount;
            this.ingredients = ingredients;
            this.core = core;
        }

        @Override
        public void apply() {
            ItemStack result = CraftTweakerMC.getItemStack(this.output);
            List<Ingredient> list = new ArrayList<>();
            for (IIngredient ingredient : this.ingredients) {
                Ingredient converted = CraftTweakerMC.getIngredient(ingredient);
                if (converted != null) {
                    list.add(converted);
                }
            }
            if (!SpellstoneTableRecipes.addRecipe(result, this.debrisCount, list, convertCore(this.core))) {
                CraftTweakerAPI.logError("[eaddons] 术石台配方无效：产出为空，或材料数不在 1~"
                        + SpellstoneTableRecipe.INGREDIENT_COUNT + " 之间");
            }
        }

        @Override
        public String describe() {
            return "添加术石台配方：" + this.output.getDisplayName();
        }

        /** 核心是原初立方时换成「必须集满」的匹配器，否则一个没铭刻的立方也能合。 */
        @Nullable
        private static Ingredient convertCore(@Nullable IIngredient core) {
            if (core == null) {
                return null;
            }
            Ingredient converted = CraftTweakerMC.getIngredient(core);
            if (converted == null) {
                return null;
            }
            for (ItemStack stack : converted.getMatchingStacks()) {
                if (stack.getItem() == ItemPrimevalCube.INSTANCE) {
                    return new CompletePrimevalCubeIngredient();
                }
            }
            return converted;
        }
    }

    private static class Remove implements IAction {
        private final IItemStack output;

        Remove(IItemStack output) {
            this.output = output;
        }

        @Override
        public void apply() {
            int removed = SpellstoneTableRecipes.removeRecipe(CraftTweakerMC.getItemStack(this.output));
            if (removed == 0) {
                CraftTweakerAPI.logWarning("[eaddons] 没有找到产出为 "
                        + this.output.getDisplayName() + " 的术石台配方");
            }
        }

        @Override
        public String describe() {
            return "移除术石台配方：" + this.output.getDisplayName();
        }
    }

    private static class RemoveAll implements IAction {
        @Override
        public void apply() {
            SpellstoneTableRecipes.removeAll();
        }

        @Override
        public String describe() {
            return "移除全部术石台配方";
        }
    }
}
