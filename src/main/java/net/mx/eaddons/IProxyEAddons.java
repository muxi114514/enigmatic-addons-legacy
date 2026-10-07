package net.mx.eaddons;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraft.world.World;

public interface IProxyEAddons {
    void init(FMLInitializationEvent event);

    void preInit(FMLPreInitializationEvent event);

    void postInit(FMLPostInitializationEvent event);

    void serverLoad(FMLServerStartingEvent event);

    /** 生成灵魂灯笼的自定义粒子（客户端有效）。flame=true 为灵魂火焰，false 为灵魂粒子。 */
    void spawnSoulParticle(World world, double x, double y, double z, boolean flame);

    /** 失落引擎抓钩挂在方块上时，由客户端钩子每 tick 调用，驱动持有者本人的绳索物理。 */
    void tickEngineRope(net.mx.eaddons.entity.EntityEngineHook hook);

    /** 本地玩家是否按住「共鸣技能键」；服务端恒为 false（服务端用客户端同步来的状态）。 */
    boolean isSpellSkillKeyDown();
}
