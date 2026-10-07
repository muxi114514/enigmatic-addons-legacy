package net.mx.eaddons.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.table.ContainerSpellstoneTable;

/** 术石工作台界面。贴图沿用 1.20.1 那张 256×256。 */
@SideOnly(Side.CLIENT)
public class GuiSpellstoneTable extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("eaddons", "textures/gui/spellstone_table.png");

    public GuiSpellstoneTable(EntityPlayer player) {
        super(new ContainerSpellstoneTable(player));
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.eaddons.spellstone_table"), 8, 4, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }
}
