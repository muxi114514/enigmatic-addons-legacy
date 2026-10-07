package net.mx.eaddons.elplus;

import java.util.Random;

import keletu.enigmaticlegacy.event.SuperpositionHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * EL+ 补回材料的来源：
 * <ul>
 *   <li>狱火余烬：七咒佩戴者击杀烈焰人；下界要塞箱子</li>
 *   <li>以太粒：玩家击杀潜影贝必掉，抢夺额外加量</li>
 *   <li>大地之心碎片：玩家击杀敌对生物小概率掉落</li>
 * </ul>
 * 只认真实玩家击杀（排除假玩家），防止刷怪塔批量产出。
 */
public class ElPlusDrops {

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote || !(event.getSource().getTrueSource() instanceof EntityPlayer)
                || event.getSource().getTrueSource() instanceof FakePlayer) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        Random rand = victim.world.rand;
        int looting = event.getLootingLevel();

        if (victim instanceof EntityBlaze && SuperpositionHandler.hasCursed(player)
                && rand.nextFloat() < ElPlusConfig.cinderBlazeChance + ElPlusConfig.cinderBlazeLootingBonus * looting) {
            int min = ElPlusConfig.cinderBlazeMin;
            int max = Math.max(min, ElPlusConfig.cinderBlazeMax);
            drop(event, ItemElPlusMaterial.INFERNAL_CINDER, min + rand.nextInt(max - min + 1));
        }

        if (victim instanceof EntityShulker) {
            int min = ElPlusConfig.nuggetShulkerMin;
            int max = Math.max(min, ElPlusConfig.nuggetShulkerMax);
            int count = min + rand.nextInt(max - min + 1) + (looting > 0 ? rand.nextInt(looting + 1) : 0);
            drop(event, ItemElPlusMaterial.ETHERIUM_NUGGET, count);
        }

        if (victim instanceof IMob
                && rand.nextFloat() < ElPlusConfig.fragmentChance + ElPlusConfig.fragmentLootingBonus * looting) {
            drop(event, ItemElPlusMaterial.EARTH_HEART_FRAGMENT, 1);
        }
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (!LootTableList.CHESTS_NETHER_BRIDGE.equals(event.getName()) || ElPlusConfig.cinderNetherChestWeight <= 0) {
            return;
        }
        LootPool pool = new LootPool(
                new LootEntry[]{
                        new LootEntryItem(ItemElPlusMaterial.INFERNAL_CINDER, ElPlusConfig.cinderNetherChestWeight, 0,
                                new LootFunction[]{new SetCount(new LootCondition[0], new RandomValueRange(1, 2))},
                                new LootCondition[0], "eaddons:infernal_cinder"),
                        new LootEntryEmpty(ElPlusConfig.cinderNetherChestEmptyWeight, 0, new LootCondition[0],
                                "eaddons:infernal_cinder_empty")
                },
                new LootCondition[0], new RandomValueRange(1), new RandomValueRange(0), "eaddons_infernal_cinder");
        event.getTable().addPool(pool);
    }

    private static void drop(LivingDropsEvent event, Item item, int count) {
        if (count <= 0) {
            return;
        }
        EntityLivingBase victim = event.getEntityLiving();
        event.getDrops().add(new EntityItem(victim.world, victim.posX, victim.posY, victim.posZ,
                new ItemStack(item, count)));
    }
}
