package net.mx.eaddons.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.client.FlowerChoicePanel;
import net.mx.eaddons.flower.FlowerChoiceMessage;
import net.mx.eaddons.flower.FlowerCoreChoice;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class GuiArtificialFlower extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("eaddons",
            "textures/gui/artificial_flower_gui.png");
    private static final ResourceLocation INVENTORY_BG = new ResourceLocation(
            "textures/gui/container/inventory.png");
    private static final DecimalFormat MODIFIER_FORMAT;
    /** 传给 renderInventoryEffect 的占位持续时间，仅用于构造 PotionEffect，不影响实际效果。 */
    private static final int EFFECT_ICON_DURATION = 200;
    /** 缓存"该药水类是否自绘图标"的判定结果，避免每帧反射。 */
    private static final Map<Class<?>, Boolean> CUSTOM_ICON_CACHE = new ConcurrentHashMap<>();

    static {
        MODIFIER_FORMAT = new DecimalFormat("#.##");
        MODIFIER_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private final ContainerArtificialFlower container;
    /** 材料栏放了术质核心时，点洗练按钮弹出的自选面板；null 表示没打开。 */
    @Nullable
    private FlowerChoicePanel panel;
    /** 本次按下已被面板处理，对应的松开 / 拖动不再交给下层槽位。 */
    private boolean swallowMouse;

    public GuiArtificialFlower(ContainerArtificialFlower container) {
        super(container);
        this.container = container;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.panel != null) {
            this.panel.layout(this.width, this.height);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.panel != null) {
            this.panel.updateCursor();
        }
    }

    private void openPanel(FlowerChoicePanel choice) {
        this.panel = choice;
        choice.layout(this.width, this.height);
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
        if (this.panel != null) {
            this.swallowMouse = true;
            if (!this.panel.contains(mouseX, mouseY)) {
                this.panel = null;
                return;
            }
            String id = this.panel.click(mouseX, mouseY, mouseButton);
            if (id != null) {
                EAddonsMod.PACKET_HANDLER.sendToServer(new FlowerChoiceMessage(this.container.windowId,
                        this.panel.isEffect(), this.panel.getIndex(), id));
                this.panel = null;
            }
            return;
        }

        int x0 = (this.width - this.xSize) / 2;
        int y0 = (this.height - this.ySize) / 2;
        ItemStack flower = ItemArtificialFlower.Helper.getFlowerStack(this.container.player, true);

        for (int id = 0; id < 3; id++) {
            int dx = mouseX - (x0 + 53);
            int dy = mouseY - (y0 + 14 + 20 * id);
            if (dx >= 0 && dy >= 0 && dx < 10 && dy < 10) {
                if (this.container.hasCore(FlowerCoreChoice.ATTRIBUTE_MATERIAL)) {
                    openPanel(FlowerChoicePanel.forAttribute(this.mc, flower, id + 1));
                    this.swallowMouse = true;
                    return;
                }
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
                if (this.container.hasCore(FlowerCoreChoice.EFFECT_MATERIAL)) {
                    openPanel(FlowerChoicePanel.forEffect(this.mc, flower, id - 4));
                    this.swallowMouse = true;
                    return;
                }
                if (this.container.enchantItem(this.mc.player, id)) {
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, id);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.swallowMouse) {
            this.swallowMouse = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.panel == null && !this.swallowMouse) {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    /** 面板打开时按键只给面板（Esc 关面板，背包键不关界面）。 */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.panel != null) {
            if (this.panel.keyTyped(typedChar, keyCode)) {
                this.panel = null;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.panel != null) {
            this.panel.scroll(wheel);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.panel != null) {
            this.panel.draw(mouseX, mouseY);
            return;
        }
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
                if (this.container.hasCore(FlowerCoreChoice.ATTRIBUTE_MATERIAL)) {
                    lines.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.flower_choice.attribute"));
                }
                this.drawHoveringText(lines, mouseX, mouseY + 5);
            }
        }

        if (mouseX >= x0 + 16 && mouseX <= x0 + 33 && mouseY >= y0 + 49 && mouseY <= y0 + 55) {
            boolean essence = this.container.hasEssence(0);
            List<String> lines = new ArrayList<>();
            lines.add(TextFormatting.GOLD + I18n.format(essence ? "gui.eaddons.artificial_flower.cost_essence"
                    : "gui.eaddons.artificial_flower.cost", this.container.materialCost(0)));
            lines.add(TextFormatting.GOLD + I18n.format("gui.eaddons.artificial_flower.cap", this.container.attributeCap(0)));
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
                if (this.container.hasCore(FlowerCoreChoice.EFFECT_MATERIAL)) {
                    lines.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.eaddons.flower_choice.effect"));
                } else if (this.container.hasEssence(FlowerCoreChoice.EFFECT_MATERIAL)) {
                    lines.add(I18n.format(id == 0 ? "tooltip.eaddons.artificial_flower.pool_advanced"
                            : "tooltip.eaddons.artificial_flower.immunity_quartz_only"));
                } else if (id == 0) {
                    lines.add(I18n.format("tooltip.eaddons.artificial_flower.pool_basic"));
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
        if (this.container.valid(0) || this.container.hasCore(FlowerCoreChoice.ATTRIBUTE_MATERIAL)) {
            this.drawTexturedModalRect(x0 + 36, y0 + 15, 192, 0, 16, 48);
        }
        boolean effectCore = this.container.hasCore(FlowerCoreChoice.EFFECT_MATERIAL);
        if (this.container.valid(1) || effectCore) {
            this.drawTexturedModalRect(x0 + 112, y0 + 22, 176, 48, 25, 8);
        }
        if (this.container.validImmunity() || effectCore) {
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
        boolean hasRing = this.container.hasRing();
        for (int id = 0; id < 2; id++) {
            Potion effect = ItemArtificialFlower.Helper.getEffect(flower, id);
            if (effect != null) {
                // 第一格是"提供"的效果，戴戒指时为 II 级；第二格是免疫，等级无意义
                int amplifier = (id == 0 && hasRing) ? 1 : 0;
                drawEffectIcon(effect, new PotionEffect(effect, EFFECT_ICON_DURATION, amplifier),
                        x0 + 140, y0 + dy);
            }
            dy += 26;
        }
    }

    /**
     * 按原版 InventoryEffectRenderer 的顺序绘制药水图标：
     * 1) 有 statusIconIndex 的（原版药水）从 inventory.png 精灵图取图；
     * 2) 无论有没有，都要调用 Forge 的 renderInventoryEffect 钩子——模组药水（含本模组的灵液腐蚀）
     *    statusIconIndex 恒为 -1，图标全靠这个钩子自绘，之前漏掉它才导致图标空白。
     * 钩子约定传入的是"效果框左上角"，实现方自己再偏移 (+6,+7)，所以这里要反向偏移对齐槽位。
     */
    private void drawEffectIcon(Potion potion, PotionEffect instance, int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        boolean drawn = false;

        if (potion.hasStatusIcon()) {
            this.mc.getTextureManager().bindTexture(INVENTORY_BG);
            int iconIndex = potion.getStatusIconIndex();
            this.drawTexturedModalRect(x, y, (iconIndex % 8) * 18, 198 + (iconIndex / 8) * 18, 18, 18);
            drawn = true;
        }

        potion.renderInventoryEffect(x - 6, y - 7, instance, this.mc);
        drawn |= hasCustomInventoryIcon(potion);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        if (!drawn) {
            // 既没有精灵图也没有自绘实现：退化成显示药水名，至少不留空白格
            String name = I18n.format(potion.getName());
            if (name.length() > 6) {
                name = name.substring(0, 5) + "…";
            }
            this.fontRenderer.drawString(name, x, y + 5, 0xCCCCCC);
        }
    }

    /** 判断药水类是否重写了 renderInventoryEffect（即自带图标）。结果按类缓存。 */
    private static boolean hasCustomInventoryIcon(Potion potion) {
        Class<?> clazz = potion.getClass();
        Boolean cached = CUSTOM_ICON_CACHE.get(clazz);
        if (cached != null) {
            return cached;
        }
        boolean custom;
        try {
            Method method = clazz.getMethod("renderInventoryEffect",
                    int.class, int.class, PotionEffect.class, Minecraft.class);
            custom = method.getDeclaringClass() != Potion.class;
        } catch (Throwable t) {
            custom = false;
        }
        CUSTOM_ICON_CACHE.put(clazz, custom);
        return custom;
    }
}
