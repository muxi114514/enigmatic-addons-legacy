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
                                new LootEntryItem(ItemArtificialFlower.INSTANCE, 10, 0, new LootFunction[0],
                                        new LootCondition[0], "eaddons:artificial_flower"),
                                new LootEntryItem(ItemAntiqueBag.INSTANCE, 20, 0, new LootFunction[0],
                                        new LootCondition[0], "eaddons:antique_bag"),
                                new LootEntryItem(ItemForgerGem.INSTANCE, 35, 0, new LootFunction[0],
                                        new LootCondition[0], "eaddons:forger_gem"),
                                new LootEntryEmpty(35, 0, new LootCondition[0], "eaddons:empty")
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
                                new LootEntryItem(ItemIchorDroplet.INSTANCE, 75, 0,
                                        new LootFunction[] {
                                                new net.minecraft.world.storage.loot.functions.SetCount(
                                                        new LootCondition[0], new RandomValueRange(1, 3))
                                        },
                                        new LootCondition[0], "eaddons:ichor_droplet"),
                                new LootEntryItem(ItemHellBladeCharm.INSTANCE, 4, 0,
                                        new LootFunction[0],
                                        new LootCondition[0], "eaddons:hell_blade_charm"),
                                new LootEntryEmpty(21, 0, new LootCondition[0], "eaddons:empty_nether")
                        },
                        new LootCondition[0],
                        new RandomValueRange(1),
                        new RandomValueRange(0, 1),
                        "eaddons_nether_loot");
                event.getTable().addPool(netherPool);
                break;
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
