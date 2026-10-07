package net.mx.eaddons.integration;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.mx.eaddons.item.ItemSpellcore;
import net.mx.eaddons.table.SpellstoneTableRecipe;

import java.util.List;

/**
 * 术石工作台的 JEI 分类。槽位摆法照搬 1.20.1：残片在左、术核居中、七种材料环绕、术石在右、成品在下。
 */
public class SpellstoneTableCategory implements IRecipeCategory<SpellstoneTableWrapper> {

    public static final String UID = "eaddons:spellstone_table";

    /** 七格材料相对配方框的坐标，与 1.20.1 一致。 */
    private static final int[][] OFFSETS = {
            {38, 52}, {31, 32}, {38, 12}, {58, 5}, {78, 12}, {85, 32}, {78, 52}
    };

    private final IDrawable background;
    private final IDrawable icon;

    public SpellstoneTableCategory(IGuiHelper guiHelper) {
        // 1.12.2 的 JEI 叫 createBlankDrawable；槽位边框由 IGuiItemStackGroup.init 自带
        this.background = guiHelper.createBlankDrawable(132, 80);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ItemSpellcore.INSTANCE));
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("gui.eaddons.jei.spellstone_crafting");
    }

    @Override
    public String getModName() {
        return "Enigmatic Addons Legacy";
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayout layout, SpellstoneTableWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup slots = layout.getItemStacks();
        slots.init(0, true, 5, 32);      // 残片
        slots.init(1, true, 58, 32);     // 术核
        for (int i = 0; i < OFFSETS.length; i++) {
            slots.init(2 + i, true, OFFSETS[i][0], OFFSETS[i][1]);
        }
        slots.init(9, false, 58, 59);    // 成品

        List<List<ItemStack>> inputs = ingredients.getInputs(mezz.jei.api.ingredients.VanillaTypes.ITEM);
        for (int i = 0; i < inputs.size() && i < 9; i++) {
            slots.set(i, inputs.get(i));
        }
        slots.set(9, wrapper.getRecipe().getResult());
    }
}
