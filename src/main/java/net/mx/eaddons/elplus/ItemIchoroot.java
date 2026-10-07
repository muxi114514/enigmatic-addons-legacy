package net.mx.eaddons.elplus;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 灵液根质（照 1.20 神遗拓展版移植）：吃下随机清除一个负面效果，25% 获得 18 秒伤害吸收，
 * 并累积「灵液值」，累积过量时触发纯化抗性（见 {@link IchorValueHandler}）。
 */
public class ItemIchoroot extends ItemFood {

    public static final ItemIchoroot INSTANCE = new ItemIchoroot();

    public ItemIchoroot() {
        super(3, 0.9F, false);
        setAlwaysEdible();
        setPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 360, 0), 0.25F);
        setUnlocalizedName("ichoroot");
        setRegistryName("ichoroot");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 21;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onFoodEaten(stack, world, player);
        if (world.isRemote) {
            return;
        }
        List<Potion> harmful = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect()) {
                harmful.add(effect.getPotion());
            }
        }
        if (!harmful.isEmpty()) {
            player.removePotionEffect(harmful.get(world.rand.nextInt(harmful.size())));
        }
        IchorValueHandler.add(player, 200 + world.rand.nextInt(100));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.eaddons.ichoroot"));
    }
}
