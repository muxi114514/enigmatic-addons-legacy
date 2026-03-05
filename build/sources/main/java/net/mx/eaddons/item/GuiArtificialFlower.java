package net.mx.eaddons.item;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class GuiArtificialFlower extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("eaddons",
            "textures/gui/artificial_flower_gui.png");
    private static final ResourceLocation INVENTORY_BG = new ResourceLocation(
            "textures/gui/container/inventory.png");
    private static final DecimalFormat MODIFIER_FORMAT;

    static {
        MODIFIER_FORMAT = new DecimalFormat("#.##");
        MODIFIER_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private final ContainerArtificialFlower container;

    public GuiArtificialFlower(ContainerArtificialFlower container) {
        super(container);
        this.container = container;
    }

    public static String getAttributeText(ItemArtificialFlower.Helper.AttributeData data) {
        AttributeModifier mod = data.modifier;
        double amount = mod.getAmount();
        if (mod.getOperation() != 0) {
            amount = amount * 100.0;
        }
        String attrName = I18n.format("attribute.name." + data.attributeName);
        if (amount > 0.0) {
            return TextFormatting.GREEN + "+" + MODIFIER_FORMAT.format(amount) + "% " + attrName;
        } else if (amount < 0.0) {
            return TextFormatting.RED + MODIFIER_FORMAT.format(amount) + "% " + attrName;
        }
        return "";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int x0 = (this.width - this.xSize) / 2;
        int y0 = (this.height - this.ySize) / 2;

        for (int id = 0; id < 3; id++) {
            int dx = mouseX - (x0 + 53);
            int dy = mouseY - (y0 + 14 + 20 * id);
            if (dx >= 0 && dy >= 0 && dx < 10 && dy < 10) {
                if (this.container.enchantItem(this.mc.player, id)) {
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, id);
                    return;
                }
            }
        }

        if (mouseX >= x0 + 16 && mouseX <= x0 + 33 && mouseY >= y0 + 49 && mouseY <= y0 + 55) {
            if (this.container.enchantItem(this.mc.player, 3)) {
                this.mc.playerController.sendEnchantPacket(this.container.windowId, 3);
                return;
            }
        }

        for (int id = 4; id < 6; id++) {
            int dx = mouseX - (x0 + 139);
            int dy = mouseY - (y0 + 16 + 26 * (id - 4));
            if (dx >= 0 && dy >= 0 && dx < 20 && dy < 20) {
                if (this.container.enchantItem(this.mc.player, id)) {
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, id);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        this.drawCustomTooltips(mouseX, mouseY);
    }

    private void drawCustomTooltips(int mouseX, int mouseY) {
        int x0 = (this.width - this.xSize) / 2;
        int y0 = (this.height - this.ySize) / 2;
        ItemStack flower = ItemArtificialFlower.Helper.getFlowerStack(this.container.player, true);

        for (int id = 0; id < 3; id++) {
            int dx = mouseX - (x0 + 53);
            int dy = mouseY - (y0 + 14 + 20 * id);
            if (dx >= 0 && dy >= 0 && dx < 10 && dy < 10) {
                ItemArtificialFlower.Helper.AttributeData data =
                        ItemArtificialFlower.Helper.getAttribute(flower, id + 1);
                List<String> lines = new ArrayList<>();
                if (data != null) {
                    lines.add(getAttributeText(data));
                } else {
                    lines.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.artificial_flower.none"));
                }
                this.drawHoveringText(lines, mouseX, mouseY + 5);
            }
        }

        if (mouseX >= x0 + 16 && mouseX <= x0 + 33 && mouseY >= y0 + 49 && mouseY <= y0 + 55) {
            int cost = this.container.costMode == 0 ? 2 : this.container.costMode == 1 ? 4 : 8;
            List<String> lines = new ArrayList<>();
            lines.add(TextFormatting.GOLD + I18n.format("gui.eaddons.artificial_flower.cost", cost));
            this.drawHoveringText(lines, mouseX, mouseY + 5);
        }

        for (int id = 0; id < 2; id++) {
            int dx = mouseX - (x0 + 139);
            int dy = mouseY - (y0 + 16 + 26 * id);
            if (dx >= 0 && dy >= 0 && dx < 20 && dy < 20) {
                Potion effect = ItemArtificialFlower.Helper.getEffect(flower, id);
                List<String> lines = new ArrayList<>();
                if (effect != null) {
                    String name = I18n.format(effect.getName());
                    TextFormatting color = effect.isBeneficial() ? TextFormatting.GREEN : TextFormatting.RED;
                    if (id == 0) {
                        boolean hasRing = this.container.hasRing();
                        String level = hasRing ? " II" : " I";
                        lines.add(color + I18n.format("tooltip.eaddons.artificial_flower.providing", name + level));
                    } else {
                        lines.add(color + I18n.format("tooltip.eaddons.artificial_flower.immunity", name));
                    }
                } else {
                    lines.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.artificial_flower.none"));
                }
                this.drawHoveringText(lines, mouseX, mouseY + 5);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x0 = (this.width - this.xSize) / 2;
        int y0 = (this.height - this.ySize) / 2;

        this.drawTexturedModalRect(x0, y0, 0, 0, this.xSize, this.ySize);

        this.fontRenderer.drawString(I18n.format("gui.eaddons.artificial_flower.attribute"),
                x0 + 10, y0 + 5, 0xFFFFFF);
        this.fontRenderer.drawString(I18n.format("gui.eaddons.artificial_flower.effect"),
                x0 + 100, y0 + 5, 0xFFFFFF);

        this.mc.getTextureManager().bindTexture(TEXTURE);
        if (this.container.valid(0)) {
            this.drawTexturedModalRect(x0 + 36, y0 + 15, 192, 0, 16, 48);
        }
        if (this.container.valid(1)) {
            this.drawTexturedModalRect(x0 + 112, y0 + 22, 176, 48, 25, 8);
            this.drawTexturedModalRect(x0 + 112, y0 + 49, 176, 56, 25, 8);
        }
        if (this.container.hasRing()) {
            this.drawTexturedModalRect(x0 + 68, y0 + 19, 176, 64, 37, 32);
        }

        this.drawTexturedModalRect(x0 + 17 + this.container.costMode * 6, y0 + 49, 176, 32, 4, 7);

        ItemStack flower = ItemArtificialFlower.Helper.getFlowerStack(this.container.player, true);
        int dy = 15;
        for (int id = 1; id <= 3; id++) {
            if (ItemArtificialFlower.Helper.getAttribute(flower, id) != null) {
                this.drawTexturedModalRect(x0 + 54, y0 + dy, 176, 0, 8, 8);
            }
            dy += 20;
        }

        dy = 17;
        for (int id = 0; id < 2; id++) {
            Potion effect = ItemArtificialFlower.Helper.getEffect(flower, id);
            if (effect != null && effect.hasStatusIcon()) {
                this.mc.getTextureManager().bindTexture(INVENTORY_BG);
                int iconIndex = effect.getStatusIconIndex();
                this.drawTexturedModalRect(x0 + 140, y0 + dy,
                        (iconIndex % 8) * 18, 198 + (iconIndex / 8) * 18, 18, 18);
                this.mc.getTextureManager().bindTexture(TEXTURE);
            }
            dy += 26;
        }
    }
}
