package net.mx.eaddons.primeval;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.table.SpellstoneTableRecipes;

import java.util.Arrays;
import java.util.List;

/**
 * 原初立方体系的两条术石台默认配方，和其他默认配方一样能被脚本的 removeAll / removeRecipe 清掉，
 * 脚本里可用 {@code addRecipe} 的第 4 个参数（核心）重新添加：
 * <ul>
 *   <li>集满的原初立方 + 寰宇之心 + 6 星尘 + 32 残片 → 原初立方（术石）</li>
 *   <li>原初立方（术石）+ 寰宇之心 + 3 以太锭 + 3 极恶锭 + 64 残片 → 非欧立方</li>
 * </ul>
 * 材料按界面槽位顺序写成「左下→左中→左上→【中上】→右上→右中→右下」，寰宇之心落在核心正上方；
 * 实际匹配是无序的，顺序只决定显示位置。
 */
public final class PrimevalTableRecipes {

    private PrimevalTableRecipes() {
    }

    public static void register() {
        Item cosmicHeart = item("enigmaticlegacy:cosmic_heart");
        Item astralDust = item("enigmaticlegacy:astral_dust");
        Item etherium = item("enigmaticlegacy:etherium_ingot");
        Item evil = item("enigmaticlegacy:evil_ingot");
        Item theCube = item("enigmaticlegacy:the_cube");
        if (cosmicHeart == null || astralDust == null || etherium == null || evil == null || theCube == null) {
            System.err.println("[EAddons] Enigmatic Legacy items missing, Primeval Cube recipes skipped");
            return;
        }

        SpellstoneTableRecipes.addRecipe(new ItemStack(ItemPrimevalSpellstone.INSTANCE), 32,
                of(astralDust, astralDust, astralDust, cosmicHeart, astralDust, astralDust, astralDust),
                new CompletePrimevalCubeIngredient());

        SpellstoneTableRecipes.addRecipe(new ItemStack(theCube), 64,
                of(etherium, etherium, etherium, cosmicHeart, evil, evil, evil),
                Ingredient.fromItem(ItemPrimevalSpellstone.INSTANCE));
    }

    private static List<Ingredient> of(Item... items) {
        Ingredient[] ingredients = new Ingredient[items.length];
        for (int i = 0; i < items.length; i++) {
            ingredients[i] = Ingredient.fromItem(items[i]);
        }
        return Arrays.asList(ingredients);
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
    }
}
