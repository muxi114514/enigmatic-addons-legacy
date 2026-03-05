package net.mx.eaddons;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.model.obj.OBJLoader;

import net.mx.eaddons.client.EtheriumCoreKeyHandler;
import net.mx.eaddons.client.EtheriumCoreShieldAuraRenderer;
import net.mx.eaddons.item.EntityDragonBreathArrow;
import net.mx.eaddons.item.RenderDragonBreathArrow;

public class ClientProxyEAddons implements IProxyEAddons {
    @Override
    public void init(FMLInitializationEvent event) {
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        OBJLoader.INSTANCE.addDomain("eaddons");
        RenderingRegistry.registerEntityRenderingHandler(EntityDragonBreathArrow.class,
                RenderDragonBreathArrow::new);
        EtheriumCoreKeyHandler.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new EtheriumCoreKeyHandler());
        MinecraftForge.EVENT_BUS.register(new EtheriumCoreShieldAuraRenderer());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Override
    public void serverLoad(FMLServerStartingEvent event) {
    }
}
