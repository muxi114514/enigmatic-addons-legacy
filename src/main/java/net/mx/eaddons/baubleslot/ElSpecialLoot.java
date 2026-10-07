package net.mx.eaddons.baubleslot;

import keletu.enigmaticlegacy.EnigmaticLegacy;
import keletu.enigmaticlegacy.util.compat.CompatBaublesEX;
import keletu.enigmaticlegacy.util.loot.SpecialLootModifierEndCity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 天体果实与灵液瓶的来源，照搬 EL 在 BaublesEX 下的注入（没装 BaublesEX 时 EL 不注册它）：
 * 七咒佩戴者每人第一次开末地城宝箱必得 1 个天体果实；灵液瓶进术石神殿池（权重 65）。
 * 不走 EL 的原入口——那个入口还会顺带往丛林神殿加失落引擎。
 */
public class ElSpecialLoot {

    private static final ResourceLocation SPELLSTONE_TEMPLE = new ResourceLocation(EnigmaticLegacy.MODID, "inject/spellstone_temple");

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (ElSlotUnlocks.handledByEnigmaticLegacy()) {
            return;
        }
        if (LootTableList.CHESTS_END_CITY_TREASURE.equals(event.getName())) {
            // EL 的条目类自带「七咒 + 每人一次」判定，直接复用
            LootEntry entry = new SpecialLootModifierEndCity(new ResourceLocation(EnigmaticLegacy.MODID, "special/end_city_treasure"),
                    1, 0, new LootCondition[0], "eaddons_special_astral_fruit");
            event.getTable().addPool(new LootPool(new LootEntry[]{entry}, new LootCondition[0],
                    new RandomValueRange(1), new RandomValueRange(1), "eaddons_special_astral_fruit"));
        } else if (SPELLSTONE_TEMPLE.equals(event.getName())) {
            LootPool pool = event.getTable().getPool("spellstones");
            if (pool != null) {
                pool.addEntry(new LootEntryItem(CompatBaublesEX.ichorBottle, 65, 0, new LootFunction[0], new LootCondition[0],
                        "eaddons_special_ichor_bottle"));
            }
        }
    }
}
