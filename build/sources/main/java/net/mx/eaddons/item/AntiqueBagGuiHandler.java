package net.mx.eaddons.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class AntiqueBagGuiHandler implements IGuiHandler {
    public static final int GUI_ID = 100;
    public static final int FLOWER_GUI_ID = 101;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            return new ContainerAntiqueBag(player);
        }
        if (ID == FLOWER_GUI_ID) {
            return new ContainerArtificialFlower(player);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            return new GuiAntiqueBag(new ContainerAntiqueBag(player));
        }
        if (ID == FLOWER_GUI_ID) {
            return new GuiArtificialFlower(new ContainerArtificialFlower(player));
        }
        return null;
    }
}
