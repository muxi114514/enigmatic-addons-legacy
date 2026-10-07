package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.flower.FlowerCoreChoice;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 石英花界面里的术质核心自选面板：覆盖在原界面上，列出可选的属性或效果，带搜索与滚动。
 * 只负责显示与点选，选中后由界面发 FlowerChoiceMessage，服务端校验后才生效。
 */
@SideOnly(Side.CLIENT)
public class FlowerChoicePanel extends Gui {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int PANEL_W = 220;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 7;
    private static final int LIST_TOP = 48;
    private static final int PANEL_H = LIST_TOP + VISIBLE_ROWS * ROW_H + 18;
    private static final int ROW_W = PANEL_W - 22;
    private static final int ICON_DURATION = 200;

    private static final class Entry {
        final String id;
        final String name;
        final String searchText;
        @Nullable
        final Potion potion;

        Entry(String id, String name, @Nullable Potion potion) {
            this.id = id;
            this.name = name;
            this.searchText = (name + " " + id).toLowerCase(Locale.ROOT);
            this.potion = potion;
        }
    }

    private final Minecraft mc;
    private final FontRenderer font;
    private final boolean effect;
    private final int index;
    private final String title;
    private final String costLine;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private final GuiTextField search;
    private int scroll;
    private int left;
    private int top;

    private FlowerChoicePanel(Minecraft mc, boolean effect, int index, String title, String costLine) {
        this.mc = mc;
        this.font = mc.fontRenderer;
        this.effect = effect;
        this.index = index;
        this.title = title;
        this.costLine = costLine;
        this.search = new GuiTextField(0, this.font, 0, 0, PANEL_W - 20, 12);
        this.search.setMaxStringLength(40);
        this.search.setFocused(true);
    }

    /** 属性条 index（1~3）的自选面板。 */
    public static FlowerChoicePanel forAttribute(Minecraft mc, ItemStack flower, int index) {
        String percent = "+" + Math.round(FlowerCoreChoice.maxAttributeValue() * 100) + "%";
        FlowerChoicePanel panel = new FlowerChoicePanel(mc, false, index,
                I18n.format("gui.eaddons.flower_choice.attribute", index),
                I18n.format("gui.eaddons.flower_choice.attribute_cost", percent));
        for (IAttribute attribute : FlowerCoreChoice.attributeChoices(mc.player, flower, index)) {
            String key = "attribute.name." + attribute.getName();
            String name = I18n.hasKey(key) ? I18n.format(key) : attribute.getName();
            panel.entries.add(new Entry(attribute.getName(), percent + " " + name, null));
        }
        panel.finish();
        return panel;
    }

    /** 效果槽 index（0 常驻、1 免疫）的自选面板。 */
    public static FlowerChoicePanel forEffect(Minecraft mc, ItemStack flower, int index) {
        boolean provide = index == FlowerCoreChoice.PROVIDE;
        FlowerChoicePanel panel = new FlowerChoicePanel(mc, true, index,
                I18n.format(provide ? "gui.eaddons.flower_choice.provide" : "gui.eaddons.flower_choice.immunity"),
                I18n.format("gui.eaddons.flower_choice.effect_cost"));
        for (Potion potion : FlowerCoreChoice.effectChoices(flower, index)) {
            ResourceLocation id = potion.getRegistryName();
            if (id != null) {
                panel.entries.add(new Entry(id.toString(), I18n.format(potion.getName()), potion));
            }
        }
        panel.finish();
        return panel;
    }

    private void finish() {
        this.entries.sort(Comparator.comparing((Entry entry) -> entry.name));
        refilter();
    }

    public boolean isEffect() {
        return this.effect;
    }

    public int getIndex() {
        return this.index;
    }

    /** 屏幕尺寸变化（initGui）时重新居中。 */
    public void layout(int screenWidth, int screenHeight) {
        this.left = (screenWidth - PANEL_W) / 2;
        this.top = (screenHeight - PANEL_H) / 2;
        this.search.x = this.left + 10;
        this.search.y = this.top + 30;
    }

    public void updateCursor() {
        this.search.updateCursorCounter();
    }

    // ============================ 绘制 ============================

    public void draw(int mouseX, int mouseY) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 400.0F);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();

        drawRect(this.left - 1, this.top - 1, this.left + PANEL_W + 1, this.top + PANEL_H + 1, 0xFF8A4FD8);
        drawRect(this.left, this.top, this.left + PANEL_W, this.top + PANEL_H, 0xF0100818);
        drawCenteredString(this.font, this.title, this.left + PANEL_W / 2, this.top + 6, 0xE0B0FF);
        drawCenteredString(this.font, this.costLine, this.left + PANEL_W / 2, this.top + 18, 0xFFD27F);
        this.search.drawTextBox();
        if (this.search.getText().isEmpty()) {
            this.font.drawString(I18n.format("gui.eaddons.flower_choice.search"), this.search.x + 4, this.search.y + 2, 0x707070);
        }

        int listTop = this.top + LIST_TOP;
        if (this.shown.isEmpty()) {
            drawCenteredString(this.font, I18n.format("gui.eaddons.flower_choice.none"),
                    this.left + PANEL_W / 2, listTop + 20, 0x888888);
        }
        int end = Math.min(this.shown.size(), this.scroll + VISIBLE_ROWS);
        for (int i = this.scroll; i < end; i++) {
            drawRow(this.shown.get(i), listTop + (i - this.scroll) * ROW_H, mouseX, mouseY);
        }
        drawScrollBar(listTop);
        drawCenteredString(this.font, I18n.format("gui.eaddons.flower_choice.back"),
                this.left + PANEL_W / 2, this.top + PANEL_H - 12, 0x707070);

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void drawRow(Entry entry, int y, int mouseX, int mouseY) {
        int x = this.left + 8;
        boolean hover = mouseX >= x && mouseX < x + ROW_W && mouseY >= y && mouseY < y + ROW_H - 2;
        drawRect(x, y, x + ROW_W, y + ROW_H - 2, hover ? 0x605A2A9A : 0x20FFFFFF);
        int textX = x + 4;
        if (entry.potion != null) {
            drawPotionIcon(entry.potion, x, y);
            textX = x + 22;
        }
        int width = x + ROW_W - textX - 2;
        int nameColor = entry.potion == null || !entry.potion.isBadEffect() ? 0xFFFFFF : 0xFF8080;
        this.font.drawString(this.font.trimStringToWidth(entry.name, width), textX, y + 1, nameColor);
        this.font.drawString(this.font.trimStringToWidth(entry.id, width), textX, y + 10, 0x8A8A8A);
    }

    /** 与原初立方记录界面同一画法：原版药水取精灵图，模组药水交给自己的 renderInventoryEffect。 */
    private void drawPotionIcon(Potion potion, int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (potion.hasStatusIcon()) {
            this.mc.getTextureManager().bindTexture(INVENTORY);
            int icon = potion.getStatusIconIndex();
            drawTexturedModalRect(x + 1, y, icon % 8 * 18, 198 + icon / 8 * 18, 18, 18);
        }
        potion.renderInventoryEffect(new PotionEffect(potion, ICON_DURATION), this, x - 5, y - 7, this.zLevel);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawScrollBar(int listTop) {
        int max = this.shown.size() - VISIBLE_ROWS;
        if (max <= 0) {
            return;
        }
        int trackX = this.left + PANEL_W - 10;
        int trackH = VISIBLE_ROWS * ROW_H - 2;
        int thumbH = Math.max(12, trackH * VISIBLE_ROWS / this.shown.size());
        int thumbY = listTop + (trackH - thumbH) * this.scroll / max;
        drawRect(trackX, listTop, trackX + 4, listTop + trackH, 0x40FFFFFF);
        drawRect(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF8A4FD8);
    }

    // ============================ 交互 ============================

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= this.left && mouseX < this.left + PANEL_W && mouseY >= this.top && mouseY < this.top + PANEL_H;
    }

    /** 左键点中某一行时返回它的注册名；点在搜索框上则聚焦它。 */
    @Nullable
    public String click(int mouseX, int mouseY, int mouseButton) {
        this.search.mouseClicked(mouseX, mouseY, mouseButton);
        int x = this.left + 8;
        int listTop = this.top + LIST_TOP;
        if (mouseButton != 0 || mouseX < x || mouseX >= x + ROW_W || mouseY < listTop) {
            return null;
        }
        int row = (mouseY - listTop) / ROW_H;
        int i = this.scroll + row;
        if (row >= VISIBLE_ROWS || i >= this.shown.size() || (mouseY - listTop) % ROW_H >= ROW_H - 2) {
            return null;
        }
        return this.shown.get(i).id;
    }

    /** 返回 true 表示要关闭面板（Esc）；其余按键交给搜索框。 */
    public boolean keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return true;
        }
        if (this.search.textboxKeyTyped(typedChar, keyCode)) {
            refilter();
        }
        return false;
    }

    public void scroll(int wheel) {
        int max = Math.max(0, this.shown.size() - VISIBLE_ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll + (wheel > 0 ? -1 : 1)));
    }

    private void refilter() {
        String query = this.search.getText().trim().toLowerCase(Locale.ROOT);
        this.shown.clear();
        for (Entry entry : this.entries) {
            if (query.isEmpty() || entry.searchText.contains(query)) {
                this.shown.add(entry);
            }
        }
        this.scroll = 0;
    }
}
