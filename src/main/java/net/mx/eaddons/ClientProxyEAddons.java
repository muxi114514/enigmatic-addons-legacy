package net.mx.eaddons;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.model.obj.OBJLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import net.mx.eaddons.client.EtheriumCoreKeyHandler;
import net.mx.eaddons.client.EtheriumCoreShieldAuraRenderer;
import net.mx.eaddons.client.ParticleSoul;
import net.mx.eaddons.client.ParticleSoulFlame;
import net.mx.eaddons.item.EntityDragonBreathArrow;
import net.mx.eaddons.item.RenderDragonBreathArrow;
import net.mx.eaddons.item.EntityQuartzDagger;
import net.mx.eaddons.item.RenderQuartzDagger;
import net.mx.eaddons.item.EntityExtradimensionalLock;
import net.mx.eaddons.item.RenderExtradimensionalLock;
import net.mx.eaddons.item.EntitySoulFlameBall;
import net.mx.eaddons.item.RenderSoulFlameBall;

public class ClientProxyEAddons implements IProxyEAddons {
    @Override
    public void init(FMLInitializationEvent event) {
        // 混沌鞘翅翅膀层：加到玩家渲染器（default/slim）的层列表，MoBends 会保留未识别的层并照常渲染
        java.util.Map<String, net.minecraft.client.renderer.entity.RenderPlayer> skinMap =
                Minecraft.getMinecraft().getRenderManager().getSkinMap();
        for (net.minecraft.client.renderer.entity.RenderPlayer renderPlayer : skinMap.values()) {
            renderPlayer.addLayer(new net.mx.eaddons.client.LayerChaosElytra(renderPlayer));
        }
    }

    @Override
    public void spawnSoulParticle(World world, double x, double y, double z, boolean flame) {
        if (!world.isRemote) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (flame) {
            mc.effectRenderer.addEffect(new ParticleSoulFlame(world, x, y, z, 0, 0, 0));
        } else {
            mc.effectRenderer.addEffect(new ParticleSoul(world, x, y, z, 0, 0, 0));
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        OBJLoader.INSTANCE.addDomain("eaddons");
        RenderingRegistry.registerEntityRenderingHandler(EntityDragonBreathArrow.class,
                RenderDragonBreathArrow::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityQuartzDagger.class,
                RenderQuartzDagger::new);
        // 封印载体是自定义实体（非 EntityItem，以脱离 ItemPhysic），自绘超维之眼模型
        RenderingRegistry.registerEntityRenderingHandler(EntityExtradimensionalLock.class,
                RenderExtradimensionalLock::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySoulFlameBall.class,
                RenderSoulFlameBall::new);
        RenderingRegistry.registerEntityRenderingHandler(net.mx.eaddons.entity.EntityAngelBeam.class,
                net.mx.eaddons.client.RenderAngelBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(net.mx.eaddons.entity.EntityEngineHook.class,
                net.mx.eaddons.client.RenderEngineHook::new);
        EtheriumCoreKeyHandler.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new EtheriumCoreKeyHandler());
        // 原初共鸣的形态切换键
        net.mx.eaddons.client.SpellstoneFormKeyHandler.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.SpellstoneFormKeyHandler());
        // 剑盾共鸣者的技能键：按住才放形态主动，不按时右键举盾
        net.mx.eaddons.client.SpellstoneSkillKeyHandler.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.SpellstoneSkillKeyHandler());
        MinecraftForge.EVENT_BUS.register(new EtheriumCoreShieldAuraRenderer());
        // 混沌鞘翅：输入/加速/粒子 + 通过 RenderPlayerEvent 兼容 mobends 挂翅膀层
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.ChaosElytraClientHandler());
        // 非欧立方主动已改版，替换 tooltip 里旧主动的描述
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.TheCubeTooltipHandler());
        // 猎宝者护符的连锁挖掘键与说明
        net.mx.eaddons.client.VeinMiningKeyHandler.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.VeinMiningKeyHandler());
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.MiningCharmTooltipHandler());
        // 比例型自定义属性在物品提示里显示为百分比
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.AttributeTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.client.SlotItemTooltipHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Override
    public void serverLoad(FMLServerStartingEvent event) {
    }

    @Override
    public void tickEngineRope(net.mx.eaddons.entity.EntityEngineHook hook) {
        net.mx.eaddons.client.EngineRopeController.tick(hook);
    }

    @Override
    public boolean isSpellSkillKeyDown() {
        return net.mx.eaddons.client.SpellstoneSkillKeyHandler.isDown();
    }
}
