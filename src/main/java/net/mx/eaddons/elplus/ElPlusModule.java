package net.mx.eaddons.elplus;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.mx.eaddons.potion.PotionPureResistance;

/**
 * 从 EL+（神秘遗物 1.21 续作）补回的物品的装配入口。
 * 配方在 assets/eaddons/recipes 下（JSON，CraftTweaker 可增删）。
 */
public final class ElPlusModule {

    public static final Item[] ITEMS = {
            ItemElPlusMaterial.INFERNAL_CINDER,
            ItemElPlusMaterial.ETHERIUM_NUGGET,
            ItemElPlusMaterial.EARTH_HEART_FRAGMENT,
            ItemExterminato.INSTANCE,
            ItemIchoroot.INSTANCE,
            ItemExecutionAxe.INSTANCE,
            BlockEternalCake.ITEM_BLOCK
    };

    public static final Block[] BLOCKS = {BlockEternalCake.INSTANCE};

    private ElPlusModule() {
    }

    public static void preInit(Configuration config) {
        ElPlusConfig.init(config);
        MinecraftForge.EVENT_BUS.register(new ElPlusDrops());
        MinecraftForge.EVENT_BUS.register(new ExecutionAxeHandler());
        MinecraftForge.EVENT_BUS.register(new IchorValueHandler());
        MinecraftForge.EVENT_BUS.register(new PotionPureResistance.Handler());
    }

    /** 物品注册完成后调用（init） */
    public static void registerOres() {
        OreDictionary.registerOre("nuggetEtherium", ItemElPlusMaterial.ETHERIUM_NUGGET);
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        for (Item item : ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
