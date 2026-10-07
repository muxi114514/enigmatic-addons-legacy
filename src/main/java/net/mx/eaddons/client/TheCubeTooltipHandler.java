package net.mx.eaddons.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.primeval.ItemPrimevalSpellstone;
import net.mx.eaddons.primeval.PrimevalConfig;

import java.util.List;

/** 非欧立方的主动已被替换（见 MixinItemTheCube），把神秘遗物 tooltip 里描述旧主动的那一行换成新描述。 */
@SideOnly(Side.CLIENT)
public class TheCubeTooltipHandler {
    private static final ResourceLocation THE_CUBE = new ResourceLocation("enigmaticlegacy", "the_cube");
    private Item theCube;

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (this.theCube == null) {
            this.theCube = ForgeRegistries.ITEMS.getValue(THE_CUBE);
        }
        if (this.theCube == null || event.getItemStack().getItem() != this.theCube) {
            return;
        }
        String old = I18n.format("tooltip.enigmaticlegacy.theCube2");
        String now = I18n.format("tooltip.eaddons.the_cube.active", PrimevalConfig.cubeBuffDuration / 1200,
                ItemPrimevalSpellstone.levelText(PrimevalConfig.buffLevelCap));
        List<String> tip = event.getToolTip();
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).contains(old)) {
                tip.set(i, tip.get(i).replace(old, now));
            }
        }
    }
}
