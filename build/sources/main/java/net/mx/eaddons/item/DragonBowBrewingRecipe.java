package net.mx.eaddons.item;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class DragonBowBrewingRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    public DragonBowBrewingRecipe() {
        setRegistryName("eaddons", "dragon_bow_brewing");
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        ItemStack bow = ItemStack.EMPTY;
        List<ItemStack> potions = new ArrayList<>();

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof ItemDragonBow) {
                    if (!bow.isEmpty()) return false;
                    bow = stack.copy();
                } else if (stack.getItem() instanceof ItemSplashPotion) {
                    potions.add(stack);
                } else {
                    return false;
                }
            }
        }

        return !bow.isEmpty() && !potions.isEmpty()
                && potions.size() <= DragonBowConfig.maxPotionAmount;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack bow = ItemStack.EMPTY;
        boolean hasWater = false;
        List<PotionEffect> effects = new ArrayList<>();

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof ItemDragonBow) {
                    if (!bow.isEmpty()) return ItemStack.EMPTY;
                    bow = stack.copy();
                } else if (stack.getItem() instanceof ItemSplashPotion) {
                    PotionType potionType = PotionUtils.getPotionFromItem(stack);
                    if (potionType == PotionType.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("water"))) {
                        hasWater = true;
                    }
                    effects.addAll(PotionUtils.getEffectsFromStack(stack));
                }
            }
        }

        if (bow.isEmpty()) return ItemStack.EMPTY;

        if (hasWater) {
            ItemDragonBow.resetEffect(bow);
            return bow;
        }

        for (PotionEffect effect : effects) {
            ItemDragonBow.addEffect(bow, effect);
        }

        return effects.isEmpty() ? ItemStack.EMPTY : bow;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
