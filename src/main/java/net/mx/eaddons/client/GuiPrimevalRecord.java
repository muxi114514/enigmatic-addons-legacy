package net.mx.eaddons.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.primeval.ItemPrimevalSpellstone;
import net.mx.eaddons.primeval.PrimevalConfig;
import net.mx.eaddons.primeval.PrimevalRecord;
import net.mx.eaddons.primeval.network.PrimevalRecordMessage;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 原初立方的记录界面：列出身上的正面效果，点选至多 N 个，关闭时提交。
 * 一个都没选就关掉则保留原记录、不进冷却；「清空记录」单独提交。
 * 列表里显示的是「按这一行记录后会给予的等级」：本术石正在给予的效果按原记录等级折算，重录不会越叠越高。
 */
@SideOnly(Side.CLIENT)
public class GuiPrimevalRecord extends GuiScreen {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int PANEL_W = 236;
    private static final int ROW_H = 22;
    private static final int VISIBLE_ROWS = 7;
    private static final int LIST_TOP = 44;

    private final Map<Potion, Integer> record;
    private final List<PotionEffect> buffs = new ArrayList<>();
    private final Set<Potion> selected = new LinkedHashSet<>();
    private int scroll;
    private boolean submitted;
    private int left;
    private int top;
    private int panelH;

    public GuiPrimevalRecord(Map<Potion, Integer> record) {
        this.record = record;
    }

    @Override
    public void initGui() {
        this.buffs.clear();
        for (PotionEffect effect : this.mc.player.getActivePotionEffects()) {
            if (!effect.getPotion().isBadEffect()) {
                this.buffs.add(effect);
            }
        }
        this.buffs.sort(Comparator.comparing(effect -> I18n.format(effect.getEffectName())));
        this.panelH = LIST_TOP + VISIBLE_ROWS * ROW_H + 34;
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - this.panelH) / 2;
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.left + 10, this.top + this.panelH - 26, 104, 20,
                I18n.format("gui.eaddons.primeval_record.confirm")));
        this.buttonList.add(new GuiButton(1, this.left + PANEL_W - 114, this.top + this.panelH - 26, 104, 20,
                I18n.format("gui.eaddons.primeval_record.clear")));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ============================ 绘制 ============================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawRect(this.left - 1, this.top - 1, this.left + PANEL_W + 1, this.top + this.panelH + 1, 0xFF8A4FD8);
        drawRect(this.left, this.top, this.left + PANEL_W, this.top + this.panelH, 0xF0100818);

        this.drawCenteredString(this.fontRenderer, I18n.format("gui.eaddons.primeval_record.title"),
                this.left + PANEL_W / 2, this.top + 8, 0xE0B0FF);
        this.fontRenderer.drawString(currentRecordLine(), this.left + 10, this.top + 20, 0xAAAAAA);
        this.fontRenderer.drawString(I18n.format("gui.eaddons.primeval_record.selected",
                this.selected.size(), PrimevalConfig.recordSlots), this.left + 10, this.top + 31, 0xFFD27F);

        if (this.buffs.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, I18n.format("gui.eaddons.primeval_record.none"),
                    this.left + PANEL_W / 2, this.top + LIST_TOP + 20, 0x888888);
        }
        int end = Math.min(this.buffs.size(), this.scroll + VISIBLE_ROWS);
        for (int i = this.scroll; i < end; i++) {
            drawRow(this.buffs.get(i), this.top + LIST_TOP + (i - this.scroll) * ROW_H, mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawRow(PotionEffect effect, int y, int mouseX, int mouseY) {
        int x = this.left + 8;
        boolean chosen = this.selected.contains(effect.getPotion());
        boolean hover = mouseX >= x && mouseX < x + PANEL_W - 16 && mouseY >= y && mouseY < y + ROW_H - 2;
        drawRect(x, y, x + PANEL_W - 16, y + ROW_H - 2, chosen ? 0x805A2A9A : hover ? 0x40FFFFFF : 0x20FFFFFF);

        Potion potion = effect.getPotion();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (potion.hasStatusIcon()) {
            this.mc.getTextureManager().bindTexture(INVENTORY);
            int index = potion.getStatusIconIndex();
            this.drawTexturedModalRect(x + 1, y + 1, index % 8 * 18, 198 + index / 8 * 18, 18, 18);
        }
        // 模组药水多半在这里画自己的图标；坐标约定是原版效果面板左上角，图标在 (+6, +7)
        potion.renderInventoryEffect(effect, this, x - 5, y - 6, this.zLevel);

        int amplifier = PrimevalRecord.recordableAmplifier(recordEntries(), effect);
        String name = I18n.format(effect.getEffectName()) + " "
                + ItemPrimevalSpellstone.levelText(effect.getAmplifier());
        String give = I18n.format("gui.eaddons.primeval_record.give",
                ItemPrimevalSpellstone.levelText(PrimevalRecord.givenAmplifier(amplifier)));
        this.fontRenderer.drawString(name, x + 24, y + 2, chosen ? 0xFFE08A : 0xFFFFFF);
        this.fontRenderer.drawString(Potion.getPotionDurationString(effect, 1.0F) + "  " + give,
                x + 24, y + 11, 0x9A9A9A);
    }

    private String currentRecordLine() {
        if (this.record.isEmpty()) {
            return I18n.format("gui.eaddons.primeval_record.current_empty");
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Potion, Integer> entry : this.record.entrySet()) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(I18n.format(entry.getKey().getName())).append(' ')
                    .append(ItemPrimevalSpellstone.levelText(PrimevalRecord.givenAmplifier(entry.getValue())));
        }
        return I18n.format("gui.eaddons.primeval_record.current", sb.toString());
    }

    private List<PrimevalRecord.Entry> recordEntries() {
        List<PrimevalRecord.Entry> entries = new ArrayList<>();
        for (Map.Entry<Potion, Integer> entry : this.record.entrySet()) {
            entries.add(new PrimevalRecord.Entry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    // ============================ 交互 ============================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int x = this.left + 8;
        int listTop = this.top + LIST_TOP;
        if (mouseButton != 0 || mouseX < x || mouseX >= x + PANEL_W - 16 || mouseY < listTop) {
            return;
        }
        int row = (mouseY - listTop) / ROW_H;
        int index = this.scroll + row;
        if (row >= VISIBLE_ROWS || index >= this.buffs.size()) {
            return;
        }
        Potion potion = this.buffs.get(index).getPotion();
        if (!this.selected.remove(potion) && this.selected.size() < PrimevalConfig.recordSlots) {
            this.selected.add(potion);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int max = Math.max(0, this.buffs.size() - VISIBLE_ROWS);
            this.scroll = Math.max(0, Math.min(max, this.scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            this.submitted = true;
            EAddonsMod.PACKET_HANDLER.sendToServer(new PrimevalRecordMessage(new ArrayList<>(), true));
        }
        this.mc.displayGuiScreen(null);
    }

    /** 关闭即提交（Esc 也算）；一个都没选就不发，保留原记录。 */
    @Override
    public void onGuiClosed() {
        if (this.submitted || this.selected.isEmpty()) {
            return;
        }
        this.submitted = true;
        List<String> ids = new ArrayList<>();
        for (Potion potion : this.selected) {
            ResourceLocation id = potion.getRegistryName();
            if (id != null) {
                ids.add(id.toString());
            }
        }
        EAddonsMod.PACKET_HANDLER.sendToServer(new PrimevalRecordMessage(ids, false));
    }
}
