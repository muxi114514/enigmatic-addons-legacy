package net.mx.eaddons;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraft.world.World;

public class ServerProxyEAddons implements IProxyEAddons {

    @Override
    public void spawnSoulParticle(World world, double x, double y, double z, boolean flame) {
        // 服务端无粒子
    }
    @Override
    public void init(FMLInitializationEvent event) {
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Override
    public void serverLoad(FMLServerStartingEvent event) {
    }

    @Override
    public void tickEngineRope(net.mx.eaddons.entity.EntityEngineHook hook) {
        // 绳索物理只在客户端
    }

    @Override
    public boolean isSpellSkillKeyDown() {
        return false;
    }
}
