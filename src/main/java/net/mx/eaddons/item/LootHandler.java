package net.mx.eaddons.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

import baubles.api.BaublesApi;

/**
 * Handles loot table injection and mob drops for eaddons items.
 */
public class LootHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        String name = event.getName().toString();
        String prefix = "minecraft:chests/";

        if (!name.startsWith(prefix))
            return;

        String file = name.substring(prefix.length());

        // Overworld dungeons: antique_bag, artificial_flower, forger_gem
        switch (file) {
            case "simple_dungeon":
            case "abandoned_mineshaft":
            case "desert_pyramid":
            case "jungle_temple":
            case "stronghold_corridor":
            case "end_city_treasure":
                LootPool overworldPool = new LootPool(
                        new LootEntry[] {
                                new LootEntryItem(ItemArtificialFlower.INSTANCE, LootConfig.overworldFlowerWeight, 0,
                                        new LootFunction[0], new LootCondition[0], "eaddons:artificial_flower"),
                                new LootEntryItem(ItemAntiqueBag.INSTANCE, LootConfig.overworldBagWeight, 0,
                                        new LootFunction[0], new LootCondition[0], "eaddons:antique_bag"),
                                new LootEntryItem(ItemForgerGem.INSTANCE, LootConfig.overworldForgerGemWeight, 0,
                                        new LootFunction[0], new LootCondition[0], "eaddons:forger_gem"),
                                new LootEntryEmpty(LootConfig.overworldEmptyWeight, 0,
                                        new LootCondition[0], "eaddons:empty")
                        },
                        new LootCondition[0],
                        new RandomValueRange(1),
                        new RandomValueRange(0, 1),
                        "eaddons_overworld_loot");
                event.getTable().addPool(overworldPool);
                break;
        }

        // Nether dungeons: ichor_droplet (1-3 count) + hell_blade_charm (rare)
        switch (file) {
            case "nether_bridge":
                LootPool netherPool = new LootPool(
                        new LootEntry[] {
                                new LootEntryItem(ItemIchorDroplet.INSTANCE, LootConfig.netherIchorWeight, 0,
                                        new LootFunction[] {
                                                new net.minecraft.world.storage.loot.functions.SetCount(
                                                        new LootCondition[0], new RandomValueRange(1, 3))
                                        },
                                        new LootCondition[0], "eaddons:ichor_droplet"),
                                new LootEntryItem(ItemHellBladeCharm.INSTANCE, LootConfig.netherHellBladeWeight, 0,
                                        new LootFunction[0],
                                        new LootCondition[0], "eaddons:hell_blade_charm"),
                                new LootEntryEmpty(LootConfig.netherEmptyWeight, 0,
                                        new LootCondition[0], "eaddons:empty_nether")
                        },
                        new LootCondition[0],
                        new RandomValueRange(1),
                        new RandomValueRange(0, 1),
                        "eaddons_nether_loot");
                event.getTable().addPool(netherPool);
                break;
        }

        addSpellstoneLoot(event, file);
    }

    /**
     * 术质共鸣者体系的箱子掉落。
     *
     * <p>概率不是拍脑袋定的，而是照搬 EL 自己术石的实际出现率：EL 把 {@code inject/<表名>} 作为一个
     * pool 挂到原版箱子上，术石在那个 pool 里以权重与其它战利品竞争（rolls 1~2）。逐表算下来是
     * 地牢/废弃矿井 8.85%、下界要塞 8.06%、沙漠神殿/丛林神庙 5.50%、要塞走廊 4.63%、末地城 4.58%。
     * 这里对每个表单开一个 pool，用「物品权重 + 空条目补足一万」精确还原同样的概率。
     *
     * <p>要塞图书馆与村庄铁匠铺本身没有术石，按最低档 4.63% 处理。
     */
    private void addSpellstoneLoot(LootTableLoadEvent event, String file) {
        int chance = spellstoneChanceOf(file);
        if (chance <= 0) {
            return;
        }
        int scaled = Math.max(0, Math.min(10000, chance * LootConfig.spellstoneLootScale / 100));
        if (scaled <= 0) {
            return;
        }

        // 术质核心：1 个
        event.getTable().addPool(pool("eaddons_spellcore_loot",
                new LootEntryItem(ItemSpellcore.INSTANCE, scaled, 0,
                        new LootFunction[0], new LootCondition[0], "eaddons:spellcore"),
                scaled));

        // 术石残片：1~3 个
        event.getTable().addPool(pool("eaddons_spellstone_debris_loot",
                new LootEntryItem(ItemSpellstoneDebris.INSTANCE, scaled, 0,
                        new LootFunction[] {
                                new net.minecraft.world.storage.loot.functions.SetCount(
                                        new LootCondition[0], new RandomValueRange(1, 3))
                        },
                        new LootCondition[0], "eaddons:spellstone_debris"),
                scaled));

        addAddonSpellstoneLoot(event, file, scaled);
    }

    /**
     * 本模组新增的两颗术石。
     * <p>幻影灵魂灯笼跟烈焰之核同表（下界要塞）；忘却冰晶走雪屋、丛林神庙、沙漠神殿。
     * 概率都与该表原有术石一致。
     */
    private void addAddonSpellstoneLoot(LootTableLoadEvent event, String file, int scaled) {
        if ("nether_bridge".equals(file)) {
            event.getTable().addPool(pool("eaddons_illusion_lantern_loot",
                    new LootEntryItem(ItemIllusionLantern.INSTANCE, scaled, 0,
                            new LootFunction[0], new LootCondition[0], "eaddons:illusion_lantern"),
                    scaled));
        }
        if ("igloo_chest".equals(file) || "jungle_temple".equals(file) || "desert_pyramid".equals(file)) {
            event.getTable().addPool(pool("eaddons_forgotten_ice_loot",
                    new LootEntryItem(ItemForgottenIce.INSTANCE, scaled, 0,
                            new LootFunction[0], new LootCondition[0], "eaddons:forgotten_ice"),
                    scaled));
        }
    }

    /** 万分制概率：命中权重 + 空条目补足 10000。 */
    private static LootPool pool(String name, LootEntryItem entry, int weight) {
        return new LootPool(
                new LootEntry[] {
                        entry,
                        new LootEntryEmpty(Math.max(0, 10000 - weight), 0, new LootCondition[0], name + "_empty")
                },
                new LootCondition[0],
                new RandomValueRange(1),
                new RandomValueRange(0, 1),
                name);
    }

    /** 各箱子里术石的实际出现率，万分制。0 表示该表不注入。 */
    private static int spellstoneChanceOf(String file) {
        switch (file) {
            case "simple_dungeon":          // 地牢
            case "abandoned_mineshaft":     // 废弃矿井（矿车）
                return 885;
            case "nether_bridge":           // 下界要塞
                return 806;
            case "desert_pyramid":          // 沙漠神殿
            case "jungle_temple":           // 丛林神庙
                return 550;
            case "stronghold_corridor":     // 要塞走廊
                return 463;
            case "end_city_treasure":       // 末地城宝库
                return 458;
            case "stronghold_library":      // 要塞图书馆：本身无术石，取最低档
            case "village_blacksmith":      // 村庄铁匠铺：同上
                return 463;
            case "igloo_chest":             // 雪屋：只为忘却冰晶开
                return 550;
            default:
                return 0;
        }
    }

    /**
     * Ichor Droplet drops from Ghasts when player has Cursed Ring equipped.
     * Also handles Hell Blade Charm special nether drop (replace ichor, once per
     * player).
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!event.isRecentlyHit())
            return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer))
            return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        if (!hasCursedRing(player) && !hasBlessedRing(player))
            return;

        if (event.getEntityLiving().getClass() == EntityGhast.class) {
            // 60% chance for first drop
            if (RANDOM.nextInt(100) < 60) {
                addDrop(event, new ItemStack(ItemIchorDroplet.INSTANCE));
            }
            // 40% chance for second drop
            if (RANDOM.nextInt(100) < 40) {
                addDrop(event, new ItemStack(ItemIchorDroplet.INSTANCE));
            }
        }
    }

    private void addDrop(LivingDropsEvent event, ItemStack drop) {
        EntityItem itemEntity = new EntityItem(
                event.getEntityLiving().world,
                event.getEntityLiving().posX,
                event.getEntityLiving().posY,
                event.getEntityLiving().posZ,
                drop);
        itemEntity.setPickupDelay(10);
        event.getDrops().add(itemEntity);
    }

    private static boolean hasCursedRing(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "cursed_ring"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }

    private static boolean hasBlessedRing(EntityPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", "blessed_ring"));
        return item != null && BaublesApi.isBaubleEquipped(player, item) != -1;
    }
}
