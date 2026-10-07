package net.mx.eaddons.attribute;

import net.minecraftforge.common.MinecraftForge;

/** 属性系统的装配入口，主类 preInit 调用一次 */
public final class AttributeModule {

    private AttributeModule() {
    }

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new EnigmaticAttributes.Registrar());
        MinecraftForge.EVENT_BUS.register(new AttributeSources.Ticker());
        MinecraftForge.EVENT_BUS.register(new CombatAttributeHandler());
        MinecraftForge.EVENT_BUS.register(new ProjectileDeflectHandler());
        MinecraftForge.EVENT_BUS.register(new MiningSpeedHandler());
        MinecraftForge.EVENT_BUS.register(new MovementAttributeHandler());
        ElAttributeSources.register();
        EAddonsAttributeSources.register();
    }
}
