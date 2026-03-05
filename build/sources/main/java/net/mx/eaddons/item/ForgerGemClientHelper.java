package net.mx.eaddons.item;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ForgerGemClientHelper {

    public static EntityPlayer findPlayerForAnvil(ItemStack eventLeft) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null && player.openContainer instanceof ContainerRepair) {
            ContainerRepair anvil = (ContainerRepair) player.openContainer;
            if (anvil.getSlot(0).getStack() == eventLeft) {
                return player;
            }
        }
        return null;
    }
}
