package net.mx.eaddons.primeval;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import javax.annotation.Nullable;

/**
 * 只认十种术石都铭刻完的原初立方。没集满的连核心槽都放不进去。
 * <p>{@link #getMatchingStacks()} 给出的是一个集满的样本，JEI 展示用。
 */
public class CompletePrimevalCubeIngredient extends Ingredient {

    public CompletePrimevalCubeIngredient() {
        super(PrimevalCubeData.completeStack());
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() == ItemPrimevalCube.INSTANCE && PrimevalCubeData.isComplete(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }
}
