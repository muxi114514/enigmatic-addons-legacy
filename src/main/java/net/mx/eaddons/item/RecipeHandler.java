package net.mx.eaddons.item;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
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

        // 大地之心两种形态（meta0 普通、meta1 被污染）在配方中通用；
        // 合并成一个 Ingredient，JEI 中心格会自动循环展示两种形态
        Ingredient anyEarthHeart = earthHeart == null ? null
                : Ingredient.fromStacks(
                        new ItemStack(earthHeart, 1, 0),
                        new ItemStack(earthHeart, 1, 1));

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
                    'X', anyEarthHeart,
                    'R', new ItemStack(Items.GLOWSTONE_DUST),
                    'E', new ItemStack(Items.ENDER_EYE));
        }

        // 11. Quartz Scepter
        //  QQ
        // IRQ  R=ghast_tear, I=lapis, S=stick, Q=quartz
        // SI
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "quartz_scepter"),
                new ResourceLocation("eaddons"),
                new ItemStack(ItemQuartzScepter.INSTANCE),
                " QQ",
                "IRQ",
                "SI ",
                'R', new ItemStack(Items.GHAST_TEAR),
                'I', new ItemStack(Items.DYE, 1, 4), // lapis lazuli
                'S', new ItemStack(Items.STICK),
                'Q', new ItemStack(Items.QUARTZ));

        // 12. Extradimensional Scepter
        //  BE
        // IRB  R=blaze_rod, I=ichor_droplet, G=gold_ingot, B=gold_block, E=extradimensional_eye
        // GI
        Item extradimensionalEye = getELItem("extradimensional_eye");
        if (extradimensionalEye != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "extradimensional_scepter"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemExtradimensionalScepter.INSTANCE),
                    " BE",
                    "IRB",
                    "GI ",
                    'R', new ItemStack(Items.BLAZE_ROD),
                    'I', new ItemStack(ItemIchorDroplet.INSTANCE),
                    'G', new ItemStack(Items.GOLD_INGOT),
                    'B', new ItemStack(Blocks.GOLD_BLOCK),
                    'E', new ItemStack(extradimensionalEye));
        }

        // 13. Revival Leaf (nature spellstone)
        // F S F
        // S X S  X=earth_heart(任一形态), S=oak sapling, F=poppy
        // F S F
        if (earthHeart != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "revival_leaf"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemRevivalLeaf.INSTANCE),
                    "FSF",
                    "SXS",
                    "FSF",
                    'X', anyEarthHeart,
                    'S', new ItemStack(Blocks.SAPLING, 1, 0),      // oak sapling
                    'F', new ItemStack(Blocks.RED_FLOWER, 1, 0));  // poppy
        }

        // 14. Illusion Lantern (soul spellstone)
        // S L S
        // L X L  X=witherite_ingot, L=soul_sand, S=glowstone_dust, C=ichor_droplet(center-bottom)
        // S C S
        if (witheriteIngot != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "illusion_lantern"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemIllusionLantern.INSTANCE),
                    "SLS",
                    "LXL",
                    "SCS",
                    'X', new ItemStack(witheriteIngot),
                    'L', new ItemStack(Blocks.SOUL_SAND),
                    'S', new ItemStack(Items.GLOWSTONE_DUST),
                    'C', new ItemStack(ItemIchorDroplet.INSTANCE));
        }

        // 15. Scorched Charm (fire curio)
        // n n
        // aXa  X=magma_heart(blazing core), n=blaze_powder, a=witherite_ingot, q=magma block, b=pure_heart
        // qbq
        Item magmaHeart = getELItem("magma_heart");
        if (magmaHeart != null && witheriteIngot != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "scorched_charm"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemScorchedCharm.INSTANCE),
                    "n n",
                    "aXa",
                    "qbq",
                    'X', new ItemStack(magmaHeart),
                    'n', new ItemStack(Items.BLAZE_POWDER),
                    'a', new ItemStack(witheriteIngot),
                    'q', new ItemStack(Blocks.MAGMA),
                    'b', new ItemStack(ItemPureHeart.INSTANCE));
        }

        // 16. Chaos Elytra (worthy-only body bauble)
        // A W A
        // Q X Q  A=cosmic_heart, W=abyssal_heart, Q=evil_ingot, X=vanilla elytra,
        // D G D  D=evil_essence, G=void_pearl
        Item cosmicHeartEL = getELItem("cosmic_heart");
        Item abyssalHeart = getELItem("abyssal_heart");
        Item evilIngot = getELItem("evil_ingot");
        Item voidPearl = getELItem("void_pearl");
        if (cosmicHeartEL != null && abyssalHeart != null && evilIngot != null
                && evilEssence != null && voidPearl != null) {
            GameRegistry.addShapedRecipe(
                    new ResourceLocation("eaddons", "chaos_elytra"),
                    new ResourceLocation("eaddons"),
                    new ItemStack(ItemChaosElytra.INSTANCE),
                    "AWA",
                    "QXQ",
                    "DGD",
                    'A', new ItemStack(cosmicHeartEL),
                    'W', new ItemStack(abyssalHeart),
                    'Q', new ItemStack(evilIngot),
                    'X', new ItemStack(Items.ELYTRA),
                    'D', new ItemStack(evilEssence),
                    'G', new ItemStack(voidPearl));
        }

        // 17. Evil Armor (upgrade from Etherium pieces)
        //  I I
        //  I A I   A = etherium piece, I = evil_ingot, E = evil_essence
        //  E E
        if (evilIngot != null && evilEssence != null) {
            Item[] etheriumPieces = {etheriumHelm, etheriumChest, etheriumLegs, etheriumBoots};
            net.minecraft.item.Item[] evilPieces = {
                    ItemEvilArmor.HELM, ItemEvilArmor.CHEST, ItemEvilArmor.LEGS, ItemEvilArmor.BOOTS};
            String[] names = {"evil_helm", "evil_chest", "evil_legs", "evil_boots"};
            for (int i = 0; i < 4; i++) {
                if (etheriumPieces[i] == null) continue;
                GameRegistry.addShapedRecipe(
                        new ResourceLocation("eaddons", names[i]),
                        new ResourceLocation("eaddons"),
                        new ItemStack(evilPieces[i]),
                        "III",
                        "IAI",
                        "EEE",
                        'A', new ItemStack(etheriumPieces[i]),
                        'I', new ItemStack(evilIngot),
                        'E', new ItemStack(evilEssence));
            }
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

        registerSpellstoneRecipes();
    }

    /**
     * 术质共鸣者体系的合成台配方。
     * <p>1.20.1 那边术核与残片来自结构与战利品，本移植不做结构，故两者都给了配方；
     * 共鸣者与术石台的配方沿用 1.20.1 的形状，只把 1.13+ 的紫水晶、哭泣黑曜石换成 1.12.2 的等价物。
     */
    private static void registerSpellstoneRecipes() {
        // 术石残片：石英块 + 末影珍珠 + 萤石粉 → 2 个
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "spellstone_debris"),
                new ResourceLocation("eaddons"),
                new ItemStack(ItemSpellstoneDebris.INSTANCE, 2),
                " G ",
                "GQG",
                " E ",
                'Q', new ItemStack(Blocks.QUARTZ_BLOCK),
                'G', new ItemStack(Items.GLOWSTONE_DUST),
                'E', new ItemStack(Items.ENDER_PEARL));

        // 术质核心：4 残片 + 4 金锭 + 末影之眼
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "spellcore"),
                new ResourceLocation("eaddons"),
                new ItemStack(ItemSpellcore.INSTANCE),
                "GDG",
                "DED",
                "GDG",
                'D', new ItemStack(ItemSpellstoneDebris.INSTANCE),
                'G', new ItemStack(Items.GOLD_INGOT),
                'E', new ItemStack(Items.ENDER_EYE));

        // 术质共鸣者（形状同 1.20.1，紫水晶换成下界石英）
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "spellstone_sword"),
                new ResourceLocation("eaddons"),
                new ItemStack(ItemSpellstoneSword.INSTANCE),
                "ADA",
                "DXD",
                " S ",
                'A', new ItemStack(Items.QUARTZ),
                'D', new ItemStack(ItemSpellstoneDebris.INSTANCE),
                'X', new ItemStack(ItemSpellcore.INSTANCE),
                'S', new ItemStack(Items.STICK));

        // 术质调谐器
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "spelltuner"),
                new ResourceLocation("eaddons"),
                new ItemStack(ItemSpelltuner.INSTANCE),
                " D ",
                "GEG",
                " D ",
                'D', new ItemStack(ItemSpellstoneDebris.INSTANCE),
                'G', new ItemStack(Items.GOLD_INGOT),
                'E', new ItemStack(Items.ENDER_EYE));

        // 术石工作台（形状同 1.20.1，哭泣黑曜石换成黑曜石）
        GameRegistry.addShapedRecipe(
                new ResourceLocation("eaddons", "spellstone_table"),
                new ResourceLocation("eaddons"),
                new ItemStack(net.mx.eaddons.table.BlockSpellstoneTable.ITEM_BLOCK),
                " X ",
                "AIA",
                "SSS",
                'X', new ItemStack(ItemSpellcore.INSTANCE),
                'A', new ItemStack(Items.ENDER_PEARL),
                'I', new ItemStack(ItemSpellstoneDebris.INSTANCE),
                'S', new ItemStack(Blocks.OBSIDIAN));
    }

    private static Item getELItem(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("enigmaticlegacy", name));
    }
}
