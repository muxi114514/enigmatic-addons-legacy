package net.mx.eaddons.item;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Registers all crafting recipes for eaddons items.
 */
public class RecipeHandler {

    public static void registerRecipes() {
        // Helper: get enigmaticlegacy items
        Item gemRing = getELItem("gem_ring");
        Item enderRod = getELItem("ender_rod");
        Item evilEssence = getELItem("evil_essence");
        Item cosmicHeart = getELItem("cosmic_heart");
        Item twistedCore = getELItem("twisted_core");
        Item miningCharm = getELItem("mining_charm");
        Item golemHeart = getELItem("golem_heart");
        Item theAcknowledgment = getELItem("the_acknowledgment");
        Item witheriteIngot = getELItem("witherite_ingot");
        Item etheriumHelm = getELItem("etherium_helm");
        Item etheriumChest = getELItem("etherium_chest");
        Item etheriumLegs = getELItem("etherium_legs");
        Item etheriumBoots = getELItem("etherium_boots");
        Item earthHeart = getELItem("earth_heart");
        Item astralDust = getELItem("astral_dust");

        // 1. Emblem of Adventurer
        // L L
        // Q X Q X=diamond_sword, Q=gold_ingot, G=emerald, L=obsidian
        // Q G Q
        if (true) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "emblem_of_adventurer"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemEmblemOfAdventurer.INSTANCE),
                    "L L",
                    "QXQ",
                    "QGQ",
                    'X', new ItemStack(Items.DIAMOND_SWORD),
                    'Q', new ItemStack(Items.GOLD_INGOT),
                    'G', new ItemStack(Items.EMERALD),
                    'L', new ItemStack(Blocks.OBSIDIAN));
        }

        // 2. Quartz Ring
        // Q L Q
        // Q X Q X=gem_ring, Q=quartz, G=ghast_tear, L=lapis
        // L G L
        if (gemRing != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "quartz_ring"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemQuartzRing.INSTANCE),
                    "QLQ",
                    "QXQ",
                    "LGL",
                    'X', new ItemStack(gemRing),
                    'Q', new ItemStack(Items.QUARTZ),
                    'G', new ItemStack(Items.GHAST_TEAR),
                    'L', new ItemStack(Items.DYE, 1, 4) // lapis lazuli
            );
        }

        // 3. Totem of Malice
        // Q
        // W G W Q=witherite_ingot, W=evil_essence, G=totem_of_undying
        // Q
        if (witheriteIngot != null && evilEssence != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "totem_of_malice"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemTotemOfMalice.INSTANCE),
                    " Q ",
                    "WGW",
                    " Q ",
                    'Q', new ItemStack(witheriteIngot),
                    'W', new ItemStack(evilEssence),
                    'G', new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        // 4. Forger Gem
        // Q
        // X Q X Q=diamond, X=iron_ingot, G=quartz_ring, L=witherite_ingot
        // L G L
        if (witheriteIngot != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "forger_gem"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemForgerGem.INSTANCE),
                    " Q ",
                    "XQX",
                    "LGL",
                    'Q', new ItemStack(Items.DIAMOND),
                    'X', new ItemStack(Items.IRON_INGOT),
                    'G', new ItemStack(ItemQuartzRing.INSTANCE),
                    'L', new ItemStack(witheriteIngot));
        }

        // 5. Dragon Bow
        // D L D
        // N X D X=dragon_head, D=dragon_breath, N=witherite_ingot, L=ender_rod
        // D L D
        if (witheriteIngot != null && enderRod != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "dragon_bow"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemDragonBow.INSTANCE),
                    "DLD",
                    "NXD",
                    "DLD",
                    'X', new ItemStack(Items.SKULL, 1, 5), // dragon_head
                    'D', new ItemStack(Items.DRAGON_BREATH),
                    'N', new ItemStack(witheriteIngot),
                    'L', new ItemStack(enderRod));
        }

        // 6. Etherium Core
        // E H E
        // C X L E=ender_rod, H=etherium_helm, C=etherium_chest, L=etherium_legs
        // E B E B=etherium_boots, X=golem_heart
        if (enderRod != null && etheriumHelm != null && etheriumChest != null
                && etheriumLegs != null && etheriumBoots != null && golemHeart != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "etherium_core"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemEtheriumCore.INSTANCE),
                    "EHE",
                    "CXL",
                    "EBE",
                    'E', new ItemStack(enderRod),
                    'H', new ItemStack(etheriumHelm),
                    'C', new ItemStack(etheriumChest),
                    'X', new ItemStack(golemHeart),
                    'L', new ItemStack(etheriumLegs),
                    'B', new ItemStack(etheriumBoots));
        }

        // 7. The Bless
        // e n e
        // a X a X=the_acknowledgment, e=gold_block, n=glowstone_dust
        // e p e a=evil_essence, p=pure_heart
        if (theAcknowledgment != null && evilEssence != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "the_bless"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemTheBless.INSTANCE),
                    "ene",
                    "aXa",
                    "epe",
                    'X', new ItemStack(theAcknowledgment),
                    'n', new ItemStack(Items.GLOWSTONE_DUST),
                    'e', new ItemStack(Blocks.GOLD_BLOCK),
                    'a', new ItemStack(evilEssence),
                    'p', new ItemStack(ItemPureHeart.INSTANCE));
        }

        // 8. Earth Promise
        // m e m
        // t X a X=gem_ring, m=golden_apple, e=cosmic_heart
        // n b n t=twisted_core, a=pure_heart, n=enchanted_golden_apple, b=mining_charm
        if (gemRing != null && cosmicHeart != null && twistedCore != null && miningCharm != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "earth_promise"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemEarthPromise.INSTANCE),
                    "mem",
                    "tXa",
                    "nbn",
                    'X', new ItemStack(gemRing),
                    'm', new ItemStack(Items.GOLDEN_APPLE, 1, 0), // golden apple
                    'e', new ItemStack(cosmicHeart),
                    't', new ItemStack(twistedCore),
                    'a', new ItemStack(ItemPureHeart.INSTANCE),
                    'n', new ItemStack(Items.GOLDEN_APPLE, 1, 1), // enchanted golden apple
                    'b', new ItemStack(miningCharm));
        }

        // 9. Pure Heart (cursed-only recipe conceptually, but in 1.12.2 we just
        // register it)
        // L
        // Q X Q L=ghast_tear, Q=ichor_droplet, X=earth_heart
        // R E R R=glowstone_dust, E=ender_eye
        if (earthHeart != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "pure_heart"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemPureHeart.INSTANCE),
                    " L ",
                    "QXQ",
                    "RER",
                    'L', new ItemStack(Items.GHAST_TEAR),
                    'Q', new ItemStack(ItemIchorDroplet.INSTANCE),
                    'X', new ItemStack(earthHeart),
                    'R', new ItemStack(Items.GLOWSTONE_DUST),
                    'E', new ItemStack(Items.ENDER_EYE));

            // Alternative recipe using earth_heart meta=1 (older Enigmatic Legacy variant)
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "pure_heart_alt"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemPureHeart.INSTANCE),
                    " L ",
                    "QXQ",
                    "RER",
                    'L', new ItemStack(Items.GHAST_TEAR),
                    'Q', new ItemStack(ItemIchorDroplet.INSTANCE),
                    'X', new ItemStack(earthHeart, 1, 1),
                    'R', new ItemStack(Items.GLOWSTONE_DUST),
                    'E', new ItemStack(Items.ENDER_EYE));
        }

        // 10. Earth Heart meta 1 (from normal earth heart + astral dust)
        if (earthHeart != null && astralDust != null) {
            GameRegistry.addShapelessRecipe(
                    new ResourceLocation("eaddons", "earth_heart_meta1"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(earthHeart, 1, 1),
                    new net.minecraft.item.crafting.Ingredient[] {
                            net.minecraft.item.crafting.Ingredient.fromStacks(new ItemStack(earthHeart, 1, 0)),
                            net.minecraft.item.crafting.Ingredient.fromStacks(new ItemStack(astralDust))
                    });
        }
    }

    private static Item getELItem(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", name));
    }
}
