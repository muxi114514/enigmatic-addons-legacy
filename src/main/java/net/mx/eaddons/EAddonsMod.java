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
import net.mx.eaddons.primeval.*;
import net.mx.eaddons.primeval.network.PrimevalOpenRecordMessage;
import net.mx.eaddons.primeval.network.PrimevalRecordMessage;
import net.mx.eaddons.potion.*;

import java.io.File;

import net.minecraftforge.fml.common.registry.EntityEntry;

@Mod(modid = EAddonsMod.MODID, version = EAddonsMod.VERSION, dependencies = "required-after:fermiumbooter;required-after:baubles;required-after:enigmaticlegacy;after:simpledifficulty;after:unlockablebauble")
public class EAddonsMod {
        public static final String MODID = "eaddons";
        public static final String VERSION = "1.4";
        public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE
                        .newSimpleChannel("eaddons:a");

        @SidedProxy(clientSide = "net.mx.eaddons.ClientProxyEAddons", serverSide = "net.mx.eaddons.ServerProxyEAddons")
        public static IProxyEAddons proxy;
        @Mod.Instance(MODID)
        public static EAddonsMod instance;

        @Mod.EventHandler
        public void preInit(FMLPreInitializationEvent event) {
                net.mx.eaddons.compat.ModCompat.init();
                MinecraftForge.EVENT_BUS.register(this);
                net.mx.eaddons.attribute.AttributeModule.preInit();
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
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.despair.LastStandEventHandler());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.despair.InsigniaFireproof());
                net.mx.eaddons.despair.LastStandYield.registerDefaults();
                MinecraftForge.EVENT_BUS.register(new ExtradimensionalScepterEventHandler());
                MinecraftForge.EVENT_BUS.register(new RevivalLeafEventHandler());
                MinecraftForge.EVENT_BUS.register(new IllusionLanternEventHandler());
                MinecraftForge.EVENT_BUS.register(new ForgottenIceEventHandler());
                MinecraftForge.EVENT_BUS.register(new PrimevalCubeLoot());
                MinecraftForge.EVENT_BUS.register(new PrimevalCubeSoulbound());
                MinecraftForge.EVENT_BUS.register(new PrimevalSpellstoneEvents());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.spellstone.SpellstoneSwordEvents());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.spellstone.ResonanceEvents());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.spellstone.PlayerSpeedTracker());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.spellstone.SkillKeyState());
                MinecraftForge.EVENT_BUS.register(new SpelltunerEventHandler());
                MinecraftForge.EVENT_BUS.register(new ScorchedCharmEventHandler());
                MinecraftForge.EVENT_BUS.register(new ChaosElytraEventHandler());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.miningcharm.VeinKeyState());
                MinecraftForge.EVENT_BUS.register(new net.mx.eaddons.miningcharm.MiningCharmVeinHandler());

                Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), MODID + ".cfg"));
                config.load();

                ForgerGemConfig.init(config);
                ArtificialFlowerConfig.init(config);
                DragonBowConfig.init(config);
                EarthPromiseConfig.init(config);
                TotemOfMaliceConfig.init(config);
                EtheriumCoreConfig.init(config);
                EmblemAdventurerConfig.init(config);
                net.mx.eaddons.despair.LastStandConfig.init(config);
                QuartzScepterConfig.init(config);
                ExtradimensionalScepterConfig.init(config);
                RevivalLeafConfig.init(config);
                IllusionLanternConfig.init(config);
                ForgottenIceConfig.init(config);
                SpellstoneSwordConfig.init(config);
                SpellstoneFormConfig.init(config);
                SpelltunerConfig.init(config);
                PrimevalConfig.init(config);
                ScorchedCharmConfig.init(config);
                ChaosElytraConfig.init(config);
        ArmorConfig.init(config);
                LootConfig.init(config);
                net.mx.eaddons.miningcharm.MiningCharmConfig.init(config);
                net.mx.eaddons.elplus.ElPlusModule.preInit(config);
                net.mx.eaddons.baubleslot.BaubleSlotModule.preInit(config);

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
                addNetworkMessage(ChaosElytraStateMessage.Handler.class,
                                ChaosElytraStateMessage.class, Side.SERVER);
                addNetworkMessage(PrimevalOpenRecordMessage.Handler.class,
                                PrimevalOpenRecordMessage.class, Side.CLIENT);
                addNetworkMessage(PrimevalRecordMessage.Handler.class,
                                PrimevalRecordMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.spellstone.SpellstoneFormSwitchMessage.Handler.class,
                                net.mx.eaddons.spellstone.SpellstoneFormSwitchMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.despair.LastStandSyncMessage.Handler.class,
                                net.mx.eaddons.despair.LastStandSyncMessage.class, Side.CLIENT);
                addNetworkMessage(net.mx.eaddons.spellstone.EngineHookDetachMessage.Handler.class,
                                net.mx.eaddons.spellstone.EngineHookDetachMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.spellstone.SkillKeyMessage.Handler.class,
                                net.mx.eaddons.spellstone.SkillKeyMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.spellstone.ShieldRaiseMessage.Handler.class,
                                net.mx.eaddons.spellstone.ShieldRaiseMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.miningcharm.VeinKeyMessage.Handler.class,
                                net.mx.eaddons.miningcharm.VeinKeyMessage.class, Side.SERVER);
                addNetworkMessage(net.mx.eaddons.flower.FlowerChoiceMessage.Handler.class,
                                net.mx.eaddons.flower.FlowerChoiceMessage.class, Side.SERVER);

                proxy.preInit(event);
        }

        @Mod.EventHandler
        public void init(FMLInitializationEvent event) {
                RecipeHandler.registerRecipes();
                net.mx.eaddons.elplus.ElPlusModule.registerOres();
                registerCursedBlessedItems();
                proxy.init(event);
        }

        /**
         * 把本模组中对应 1.20 ICursed/IBlessed 的物品注册进 EnigmaticLegacy 的
         * "受诅咒/受祝福物品"配置表，使其沿用 EL 的专属限制机制
         * （tooltip、佩戴/使用强制、受祝福豁免）。
         * 在 init 阶段执行，此时 EL 已在其 preInit 加载完配置。
         */
        private void registerCursedBlessedItems() {
                // ICursed：受诅咒者可用（仅在诅咒表则受祝福者不可用）
                addToList(true, "scorched_charm", "earth_promise", "pure_heart", "the_bless", "totem_of_malice");
                // IBlessed：受祝福者也可用
                addToList(false, "scorched_charm", "earth_promise", "pure_heart", "the_bless");
                // IEldritch：仅 Worthy One 可用（混沌鞘翅），沿用 EL 的 eldritch 机制/tooltip
                ResourceLocation elytra = new ResourceLocation(MODID, "chaos_elytra");
                if (!keletu.enigmaticlegacy.EnigmaticConfigs.eldritchItemList.contains(elytra)) {
                        keletu.enigmaticlegacy.EnigmaticConfigs.eldritchItemList.add(elytra);
                }
        }

        private static void addToList(boolean cursed, String... names) {
                java.util.List<ResourceLocation> list = cursed
                                ? keletu.enigmaticlegacy.EnigmaticConfigs.cursedItemList
                                : keletu.enigmaticlegacy.EnigmaticConfigs.blessedItemList;
                for (String name : names) {
                        ResourceLocation rl = new ResourceLocation(MODID, name);
                        if (!list.contains(rl)) {
                                list.add(rl);
                        }
                }
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
                                ItemIchorDroplet.INSTANCE,
                                ItemQuartzScepter.INSTANCE,
                                ItemQuartzDagger.INSTANCE,
                                ItemExtradimensionalScepter.INSTANCE,
                                ItemRevivalLeaf.INSTANCE,
                                ItemIllusionLantern.INSTANCE,
                                ItemForgottenIce.INSTANCE,
                                ItemSpellstoneSword.INSTANCE,
                                ItemSpellcore.INSTANCE,
                                ItemSpellstoneDebris.INSTANCE,
                                ItemSpelltuner.INSTANCE,
                                ItemPrimevalCube.INSTANCE,
                                ItemPrimevalSpellstone.INSTANCE,
                                net.mx.eaddons.table.BlockSpellstoneTable.ITEM_BLOCK,
                                ItemSoulFlameBall.INSTANCE,
                                ItemScorchedCharm.INSTANCE,
                                ItemChaosElytra.INSTANCE,
                ItemEvilArmor.HELM,
                ItemEvilArmor.CHEST,
                ItemEvilArmor.LEGS,
                ItemEvilArmor.BOOTS);
                event.getRegistry().registerAll(net.mx.eaddons.elplus.ElPlusModule.ITEMS);
        }

        @SubscribeEvent
        public void registerBlocks(RegistryEvent.Register<net.minecraft.block.Block> event) {
                event.getRegistry().register(net.mx.eaddons.table.BlockSpellstoneTable.INSTANCE);
                event.getRegistry().registerAll(net.mx.eaddons.elplus.ElPlusModule.BLOCKS);
        }

        @SubscribeEvent
        public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(EntityDragonBreathArrow.class)
                                                .id(new ResourceLocation("eaddons", "dragon_breath_arrow"), 1)
                                                .name("dragon_breath_arrow").tracker(64, 1, true).build());
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(EntityQuartzDagger.class)
                                                .id(new ResourceLocation("eaddons", "quartz_dagger"), 2)
                                                .name("quartz_dagger").tracker(64, 1, true).build());
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(EntityExtradimensionalLock.class)
                                                .id(new ResourceLocation("eaddons", "extradimensional_lock"), 3)
                                                .name("extradimensional_lock").tracker(64, 3, true).build());
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(EntitySoulFlameBall.class)
                                                .id(new ResourceLocation("eaddons", "soul_flame_ball"), 4)
                                                .name("soul_flame_ball").tracker(64, 1, true).build());
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(net.mx.eaddons.entity.EntityAngelBeam.class)
                                                .id(new ResourceLocation("eaddons", "angel_beam"), 5)
                                                .name("angel_beam").tracker(64, 1, true).build());
                // 抓钩飞行时每 tick 都在动，勾中后持有者客户端要立刻拿到落点，更新频率给 1、开启速度同步
                event.getRegistry().register(
                                EntityEntryBuilder.create().entity(net.mx.eaddons.entity.EntityEngineHook.class)
                                                .id(new ResourceLocation("eaddons", "engine_hook"), 6)
                                                .name("engine_hook").tracker(96, 1, true).build());
        }

        @SubscribeEvent
        public void registerEnchantments(RegistryEvent.Register<net.minecraft.enchantment.Enchantment> event) {
                event.getRegistry().register(EnchantmentMultishot.INSTANCE);
        }

        @SubscribeEvent
        public void registerPotions(RegistryEvent.Register<Potion> event) {
                event.getRegistry().registerAll(
                                PotionIchorCorrosion.INSTANCE,
                                PotionDragonBreath.INSTANCE,
                                net.mx.eaddons.potion.PotionPureResistance.INSTANCE);
        }

        @SubscribeEvent
        public void registerRecipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> event) {
                event.getRegistry().register(new DragonBowBrewingRecipe());
                // 术石台配方表在这里建：preInit 时别的模组的物品还没注册（注册事件在 preInit 之后才触发），
                // 而 CraftTweaker 在本事件的 LOWEST 执行脚本，默认优先级正好排在它之前，脚本的增删不会被覆盖。
                net.mx.eaddons.table.SpellstoneTableRecipes.init();
                // 原初立方的两条默认配方（脚本的 removeAll 会一并清掉）
                PrimevalTableRecipes.register();
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
                ModelLoader.setCustomModelResourceLocation(ItemQuartzScepter.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:quartz_scepter", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemQuartzDagger.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:quartz_dagger", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemExtradimensionalScepter.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:extradimensional_scepter", "inventory"));
                // 复苏之叶注册名在 enigmaticlegacy 域（继承 EL 咒石基类所致），模型单独指到 eaddons
                ModelLoader.setCustomModelResourceLocation(ItemRevivalLeaf.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:revival_leaf", "inventory"));
                // 灵魂灯笼注册名在 enigmaticlegacy 域（继承 EL 咒石基类），模型单独指到 eaddons
                ModelLoader.setCustomModelResourceLocation(ItemIllusionLantern.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:illusion_lantern", "inventory"));
                // 忘却冰晶同理：注册名在 enigmaticlegacy 域，模型单独指到 eaddons
                ModelLoader.setCustomModelResourceLocation(ItemForgottenIce.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:forgotten_ice", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemPrimevalCube.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:primeval_cube", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemPrimevalSpellstone.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:primeval_spellstone", "inventory"));
                net.mx.eaddons.elplus.ElPlusModule.registerModels();
                ModelLoader.setCustomModelResourceLocation(ItemSpellstoneSword.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:spellstone_sword", "inventory"));
                // 按形态换贴图的属性覆写是客户端专有 API，放在客户端类里注册
                net.mx.eaddons.client.SpellstoneSwordProperties.register();
                ModelLoader.setCustomModelResourceLocation(ItemSpellcore.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:spellcore", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemSpellstoneDebris.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:spellstone_debris", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemSpelltuner.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:spelltuner", "inventory"));
                ModelLoader.setCustomModelResourceLocation(net.mx.eaddons.table.BlockSpellstoneTable.ITEM_BLOCK, 0,
                                new ModelResourceLocation("eaddons:spellstone_table", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemSoulFlameBall.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:soul_flame_ball", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemScorchedCharm.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:scorched_charm", "inventory"));
                ModelLoader.setCustomModelResourceLocation(ItemChaosElytra.INSTANCE, 0,
                                new ModelResourceLocation("eaddons:chaos_elytra", "inventory"));
                for (ItemEvilArmor piece : new ItemEvilArmor[]{ItemEvilArmor.HELM, ItemEvilArmor.CHEST,
                                ItemEvilArmor.LEGS, ItemEvilArmor.BOOTS}) {
                        ModelLoader.setCustomModelResourceLocation(piece, 0,
                                        new ModelResourceLocation(piece.getRegistryName(), "inventory"));
                }
        }

        private int messageID = 0;

        public <T extends IMessage, V extends IMessage> void addNetworkMessage(
                        Class<? extends IMessageHandler<T, V>> handler, Class<T> messageClass, Side... sides) {
                for (Side side : sides)
                        PACKET_HANDLER.registerMessage(handler, messageClass, messageID, side);
                messageID++;
        }
}
