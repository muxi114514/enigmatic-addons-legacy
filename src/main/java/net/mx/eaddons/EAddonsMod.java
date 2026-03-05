package net.mx.eaddons;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.config.Configuration;

import net.mx.eaddons.item.*;
import net.mx.eaddons.potion.*;

import java.io.File;

import net.minecraftforge.fml.common.registry.EntityEntry;

@Mod(modid = EAddonsMod.MODID, version = EAddonsMod.VERSION, dependencies = "required-after:fermiumbooter;required-after:baubles;required-after:enigmaticlegacy")
public class EAddonsMod {
        public static final String MODID = "eaddons";
        public static final String VERSION = "1.0";
        public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE
                        .newSimpleChannel("eaddons:a");

        @SidedProxy(clientSide = "net.mx.eaddons.ClientProxyEAddons", serverSide = "net.mx.eaddons.ServerProxyEAddons")
        public static IProxyEAddons proxy;
        @Mod.Instance(MODID)
        public static EAddonsMod instance;

        @Mod.EventHandler
        public void preInit(FMLPreInitializationEvent event) {
                MinecraftForge.EVENT_BUS.register(this);
                MinecraftForge.EVENT_BUS.register(new AntiqueBagEventHandler());
                MinecraftForge.EVENT_BUS.register(new LootHandler());
                MinecraftForge.EVENT_BUS.register(new HellBladeCharmEventHandler());
                MinecraftForge.EVENT_BUS.register(new TheBlessEventHandler());
                MinecraftForge.EVENT_BUS.register(new ForgerGemEventHandler());
                MinecraftForge.EVENT_BUS.register(new QuartzRingEventHandler());
                MinecraftForge.EVENT_BUS.register(new ArtificialFlowerEventHandler());
                MinecraftForge.EVENT_BUS.register(new EarthPromiseEventHandler());
                MinecraftForge.EVENT_BUS.register(new EtheriumCoreEventHandler());
                MinecraftForge.EVENT_BUS.register(new TotemOfMaliceEventHandler());
                MinecraftForge.EVENT_BUS.register(new EmblemAdventurerEventHandler());

                Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), MODID + ".cfg"));
                config.load();

                ForgerGemConfig.init(config);
                ArtificialFlowerConfig.init(config);
                DragonBowConfig.init(config);
                EarthPromiseConfig.init(config);
                TotemOfMaliceConfig.init(config);
                EtheriumCoreConfig.init(config);
                EmblemAdventurerConfig.init(config);

                if (config.hasChanged()) {
                        config.save();
                }

                NetworkRegistry.INSTANCE.registerGuiHandler(this, new AntiqueBagGuiHandler());

                addNetworkMessage(EarthPromiseCooldownMessage.Handler.class,
                                EarthPromiseCooldownMessage.class, Side.CLIENT);
                addNetworkMessage(EtheriumCoreTriggerMessage.Handler.class,
                                EtheriumCoreTriggerMessage.class, Side.SERVER);
                addNetworkMessage(EtheriumCoreSyncMessage.Handler.class,
                                EtheriumCoreSyncMessage.class, Side.CLIENT);

                proxy.preInit(event);
        }

        @Mod.EventHandler
        public void init(FMLInitializationEvent event) {
                RecipeHandler.registerRecipes();
                proxy.init(event);
        }

        @Mod.EventHandler
        public void postInit(FMLPostInitializationEvent event) {
                proxy.postInit(event);
        }

        @Mod.EventHandler
        public void serverLoad(FMLServerStartingEvent event) {
                proxy.serverLoad(event);
        }

        @SubscribeEvent
        public void registerItems(RegistryEvent.Register<Item> event) {
                event.getRegistry().registerAll(
                                ItemAntiqueBag.INSTANCE,
                                ItemHellBladeCharm.INSTANCE,
                                ItemTheBless.INSTANCE,
                                ItemForgerGem.INSTANCE,
                                ItemQuartzRing.INSTANCE,
                                ItemArtificialFlower.INSTANCE,
                                ItemDragonBow.INSTANCE,
                                ItemEarthPromise.INSTANCE,
                                ItemTotemOfMalice.INSTANCE,
                                ItemEtheriumCore.INSTANCE,
                                ItemEmblemOfAdventurer.INSTANCE,
                                ItemInsigniaOfDespair.INSTANCE,
                                ItemPureHeart.INSTANCE,
                                ItemIchorDroplet.INSTANCE);
        }

        @SubscribeEvent
        public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(EntityDragonBreathArrow.class)
                                                .id(new ResourceLocation("eaddons", "dragon_breath_arrow"), 1)
                                                .name("dragon_breath_arrow").tracker(64, 1, true).build());
        }

        @SubscribeEvent
        public void registerEnchantments(RegistryEvent.Register<net.minecraft.enchantment.Enchantment> event) {
                event.getRegistry().register(EnchantmentMultishot.INSTANCE);
        }

        @SubscribeEvent
        public void registerPotions(RegistryEvent.Register<Potion> event) {
                event.getRegistry().registerAll(
                                PotionIchorCorrosion.INSTANCE,
                                PotionDragonBreath.INSTANCE);
        }

        @SubscribeEvent
        public void registerRecipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> event) {
                event.getRegistry().register(new DragonBowBrewingRecipe());
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void registerModels(ModelRegistryEvent event) {
                ModelLoader.setCustomModelResourceLocation(ItemAntiqueBag.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:antique_bag", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemHellBladeCharm.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:hell_blade_charm", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemTheBless.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:the_bless", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemForgerGem.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:forger_gem", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemQuartzRing.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:quartz_ring", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemArtificialFlower.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:artificial_flower", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemDragonBow.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:dragon_bow", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemEarthPromise.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:earth_promise", "inventory"));
                ItemTotemOfMalice.registerModels(event);
                ModelLoader.setCustomModelResourceLocation(ItemEtheriumCore.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:etherium_core", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemEmblemOfAdventurer.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:emblem_of_adventurer", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemInsigniaOfDespair.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:insignia_of_despair", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemPureHeart.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:pure_heart", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemIchorDroplet.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:ichor_droplet", "inventory"));
        }

        private int messageID = 0;

        public <T extends IMessage, V extends IMessage> void addNetworkMessage(
                        Class<? extends IMessageHandler<T, V>> handler, Class<T> messageClass, Side... sides) {
                for (Side side : sides)
                        PACKET_HANDLER.registerMessage(handler, messageClass, messageID, side);
                messageID++;
        }
}
