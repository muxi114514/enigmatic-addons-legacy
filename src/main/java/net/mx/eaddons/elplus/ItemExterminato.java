package net.mx.eaddons.elplus;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/**
 * 烬灭薯：吃下后 1/3 无事、1/3 耀炎之力 I、1/3 耀炎之力 II（各 30 秒）。
 * <p>EL+ 用的「烈焰巨力」就是 EL 1.12 耀炎之力的后续版本（同样加攻击、受伤即消失），直接复用。
 */
public class ItemExterminato extends ItemFood {

    public static final ItemExterminato INSTANCE = new ItemExterminato();

    private static final int DURATION = 600;

    public ItemExterminato() {
        super(7, 0.7F, false);
        setAlwaysEdible();
        setUnlocalizedName("exterminato");
        setRegistryName("exterminato");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        int roll = world.rand.nextInt(3);
        if (roll > 0) {
            player.addPotionEffect(new PotionEffect(EnigmaticLegacy.blazingStrengthEffect, DURATION, roll - 1));
        }
    }
}
