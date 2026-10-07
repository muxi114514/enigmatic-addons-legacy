package net.mx.eaddons.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.miningcharm.MiningCharmConfig;

import java.util.List;

/** 猎宝者护符按住 Shift 的说明：夜视条件改成永久，末尾（夜视那几行之后）补上连锁挖掘。 */
@SideOnly(Side.CLIENT)
public class MiningCharmTooltipHandler {
    private static final ResourceLocation MINING_CHARM = new ResourceLocation("enigmaticlegacy", "mining_charm");
    private Item charm;

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (this.charm == null) {
            this.charm = ForgeRegistries.ITEMS.getValue(MINING_CHARM);
        }
        if (this.charm == null || event.getItemStack().getItem() != this.charm) {
            return;
        }
        List<String> tip = event.getToolTip();
        if (MiningCharmConfig.permanentNightVision) {
            replaceLine(tip, I18n.format("tooltip.enigmaticlegacy.miningCharm3"),
                    I18n.format("tooltip.eaddons.mining_charm.night_vision"));
        }
        if (MiningCharmConfig.veinMiningEnabled) {
            appendVeinMining(tip);
        }
    }

    private static void replaceLine(List<String> tip, String old, String now) {
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).equals(old)) {
                tip.set(i, now);
                return;
            }
        }
    }

    /** 只有按住 Shift 时 EL 才列出详细说明，以它的最后一行为锚点。 */
    private static void appendVeinMining(List<String> tip) {
        String anchor = I18n.format("tooltip.enigmaticlegacy.miningCharm6");
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).contains(anchor)) {
                String key = VeinMiningKeyHandler.veinKey.getDisplayName();
                tip.add(i + 1, "");
                tip.add(i + 2, I18n.format("tooltip.eaddons.mining_charm.vein1", key));
                tip.add(i + 3, I18n.format("tooltip.eaddons.mining_charm.vein2", MiningCharmConfig.veinMaxBlocks));
                tip.add(i + 4, I18n.format("tooltip.eaddons.mining_charm.vein3"));
                return;
            }
        }
    }
}
