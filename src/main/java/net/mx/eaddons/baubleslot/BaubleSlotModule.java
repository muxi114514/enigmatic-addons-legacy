package net.mx.eaddons.baubleslot;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

/** 天体果实 / 灵液瓶始终注册与开槽的装配入口（物品注册与开槽逻辑见 mixin 包 MixinElObjectRegistry 等） */
public final class BaubleSlotModule {

    private BaubleSlotModule() {
    }

    public static void preInit(Configuration config) {
        BaubleSlotConfig.init(config);
        MinecraftForge.EVENT_BUS.register(new ElSpecialLoot());
    }
}
