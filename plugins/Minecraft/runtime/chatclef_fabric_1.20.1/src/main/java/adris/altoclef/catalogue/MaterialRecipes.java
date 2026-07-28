package adris.altoclef.catalogue;

import adris.altoclef.catalogue.TaskCatalogue.CataloguedResource;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Entities;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.tasks.resources.wood.*;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Items;

//20260728_kpopmodder: Split catalogue registrations by responsibility to keep TaskCatalogue maintainable.
@SuppressWarnings({"rawtypes"})
final class MaterialRecipes extends TaskRegistrar {
    private MaterialRecipes() {
    }

    static void register() {
        String p = "planks";
        String s = "stick";
        String o = null;

        // MATERIALS
        //mine("netherite_upgrade_smithing_template", MiningRequirement.HAND, Blocks.CHEST, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).forceDimension(Dimension.NETHER);
        simple("netherite_upgrade_smithing_template", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, GetSmithingTemplateTask::new);
        alias("netherite_upgrade", "netherite_upgrade_smithing_template");
        simple("planks", ItemHelper.PLANKS, CollectPlanksTask::new).dontMineIfPresent();
        // Per-tree Planks. At the moment, nether planks need to be specified that their logs are in the nether.
        for (CataloguedResource woodCatalogue : woodTasks("planks", wood -> wood.planks, (wood, count) -> {
            CollectPlanksTask result = new CollectPlanksTask(wood.planks, count);
            if (wood.isNetherWood()) {
                // Kinda jank
                result.logsInNether();
            }
            return result;
        }, true)) {
            // Don't mine individual planks either!! Handled internally.
            woodCatalogue.dontMineIfPresent();
        }
        simple("stripped_logs", ItemHelper.STRIPPED_LOGS, CollectStrippedLogTask::new).dontMineIfPresent();
        for (CataloguedResource woodCatalogue : woodTasks("stripped_logs", wood -> wood.strippedLog,
                (wood, count) -> new CollectStrippedLogTask(wood.strippedLog, count))) {
            woodCatalogue.dontMineIfPresent();
        }
        // shapedRecipe2x2("stick", Items.STICK, 4, p, o, p, o);
        simple("stick", Items.STICK, CollectSticksTask::new);
        smelt("stone", Items.STONE, "cobblestone").dontMineIfPresent();
        smelt("deepslate", Items.DEEPSLATE, "cobbled_deepslate").dontMineIfPresent();
        smelt("smooth_stone", Items.SMOOTH_STONE, "stone");
        smelt("smooth_quartz", Items.SMOOTH_QUARTZ, "quartz_block");
        smelt("smooth_basalt", Items.SMOOTH_BASALT, "basalt");
        smelt("glass", Items.GLASS, "sand").dontMineIfPresent();
        simple("iron_ingot", Items.IRON_INGOT, CollectIronIngotTask::new).forceDimension(Dimension.OVERWORLD);
        smelt("copper_ingot", Items.COPPER_INGOT, "raw_copper", Items.COPPER_ORE);
        smelt("charcoal", Items.CHARCOAL, "log");
        smelt("brick", Items.BRICK, "clay_ball");
        smelt("nether_brick", Items.NETHER_BRICK, "netherrack");
        smelt("green_dye", Items.GREEN_DYE, "cactus");
        simple("gold_ingot", Items.GOLD_INGOT, CollectGoldIngotTask::new).anyDimension(); // accounts for nether too
        shapedRecipe3x3Block("iron_block", Items.IRON_BLOCK, "iron_ingot");
        shapedRecipe3x3Block("gold_block", Items.GOLD_BLOCK, "gold_ingot");
        shapedRecipe3x3Block("copper_block", Items.COPPER_BLOCK, "copper_ingot");
        shapedRecipe3x3Block("raw_iron_block", Items.RAW_IRON_BLOCK, "raw_iron");
        shapedRecipe3x3Block("raw_gold_block", Items.RAW_GOLD_BLOCK, "raw_gold");
        shapedRecipe3x3Block("raw_copper_block", Items.RAW_COPPER_BLOCK, "raw_copper");
        shapedRecipe3x3Block("diamond_block", Items.DIAMOND_BLOCK, "diamond");
        shapedRecipe3x3Block("redstone_block", Items.REDSTONE_BLOCK, "redstone");
        shapedRecipe3x3Block("coal_block", Items.COAL_BLOCK, "coal");
        shapedRecipe3x3Block("emerald_block", Items.EMERALD_BLOCK, "emerald");
        shapedRecipe3x3Block("lapis_block", Items.LAPIS_BLOCK, "lapis_lazuli");
        shapedRecipe3x3Block("slime_block", Items.SLIME_BLOCK, "slime_ball");
        shapedRecipe3x3Block("melon", Items.MELON, "melon_slice").dontMineIfPresent();
        shapedRecipe2x2Block("glowstone", Items.GLOWSTONE, "glowstone_dust").dontMineIfPresent();
        shapedRecipe2x2Block("clay", Items.CLAY, "clay_ball").dontMineIfPresent();
        smelt("netherite_scrap", Items.NETHERITE_SCRAP, "ancient_debris");
        shapedRecipe3x3("netherite_ingot", Items.NETHERITE_INGOT, 1, "netherite_scrap", "netherite_scrap", "netherite_scrap", "netherite_scrap", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", o);
        simple("gold_nugget", Items.GOLD_NUGGET, CollectGoldNuggetsTask::new);
        {
            String g = "gold_nugget";
            shapedRecipe3x3("glistering_melon_slice", Items.GLISTERING_MELON_SLICE, 1, g, g, g, g, "melon_slice", g, g, g, g);
        }
        shapedRecipe2x2("sugar", Items.SUGAR, 1, "sugar_cane", o, o, o);
        shapedRecipe2x2("bone_meal", Items.BONE_MEAL, 3, "bone", o, o, o);
        shapedRecipe2x2("melon_seeds", Items.MELON_SEEDS, 1, "melon_slice", o, o, o);
        shapedRecipe2x2("bamboo_planks", Items.BAMBOO_PLANKS, 2, "bamboo_block", o, o, o);
        shapedRecipe3x3Block("bamboo_block", Items.BAMBOO_BLOCK, "bamboo");
        simple("hay_block", Items.HAY_BLOCK, CollectHayBlockTask::new).dontMineIfPresent();
        shapedRecipe2x2Block("polished_andesite", Items.POLISHED_ANDESITE, 4, "andesite");
        shapedRecipe2x2Block("polished_diorite", Items.POLISHED_DIORITE, 4, "diorite");
        shapedRecipe2x2Block("polished_granite", Items.POLISHED_GRANITE, 4, "granite");
        shapedRecipe2x2Block("quartz_block", Items.QUARTZ_BLOCK, "quartz");
        shapedRecipe2x2Block("polished_blackstone", Items.POLISHED_BLACKSTONE, 4, "blackstone");
        shapedRecipe2x2Block("polished_blackstone_bricks", Items.POLISHED_BLACKSTONE_BRICKS, 4, "polished_blackstone");
        shapedRecipe2x2Block("polished_basalt", Items.POLISHED_BASALT, 4, "basalt");
        shapedRecipe2x2Block("polished_deepslate", Items.POLISHED_DEEPSLATE, 4, "cobbled_deepslate");
        shapedRecipe2x2Block("deepslate_bricks", Items.DEEPSLATE_BRICKS, 4, "polished_deepslate");
        shapedRecipe2x2Block("deepslate_tiles", Items.DEEPSLATE_TILES, 4, "deepslate_bricks");
        shapedRecipe2x2Block("cut_copper", Items.CUT_COPPER, 4, "copper_block");
        shapedRecipe2x2Block("cut_sandstone", Items.CUT_SANDSTONE, 4, "sandstone");
        shapedRecipe2x2Block("cut_red_sandstone", Items.CUT_RED_SANDSTONE, 4, "red_sandstone");
        shapedRecipe2x2Block("quartz_bricks", Items.QUARTZ_BRICKS, 4, "quartz_block");
        shapedRecipe2x2("quartz_pillar", Items.QUARTZ_PILLAR, 4, "quartz_block", o, "quartz_block", o);
        shapedRecipe2x2Block("stone_bricks", Items.STONE_BRICKS, 4, "stone");
        shapedRecipe2x2("mossy_stone_bricks", Items.MOSSY_STONE_BRICKS, 1, "stone_bricks", "vine", o, o);
        shapedRecipe2x2("mossy_cobblestone", Items.MOSSY_COBBLESTONE, 1, "cobblestone", "vine", o, o);
        simple("nether_bricks", Items.NETHER_BRICKS, CollectNetherBricksTask::new).dontMineIfPresent();
        shapedRecipe2x2Block("red_nether_bricks", Items.RED_NETHER_BRICKS, 4, "nether_wart");
        smelt("cracked_stone_bricks", Items.CRACKED_STONE_BRICKS, "stone_bricks");
        smelt("cracked_nether_bricks", Items.CRACKED_NETHER_BRICKS, "nether_bricks");
        smelt("cracked_polished_blackstone_bricks", Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, "polished_blackstone_bricks");
        smelt("cracked_deepslate_bricks", Items.CRACKED_DEEPSLATE_BRICKS, "deepslate_bricks");
        smelt("cracked_deepslate_tiles", Items.CRACKED_DEEPSLATE_TILES, "deepslate_tiles");
        smelt("smooth_sandstone", Items.SMOOTH_SANDSTONE, "sandstone");
        smelt("smooth_red_sandstone", Items.SMOOTH_RED_SANDSTONE, "red_sandstone");
        {
            String B = "nether_bricks";
            String b = "nether_brick";
            shapedRecipe3x3("nether_brick_fence", Items.NETHER_BRICK_FENCE, 6, o, o, o, B, b, B, B, b, B);
        }
        shapedRecipe3x3("brush", Items.BRUSH, 1, o, "feather", o, o, "copper_ingot", o, o, s, o);
        shapedRecipe3x3("paper", Items.PAPER, 3, "sugar_cane", "sugar_cane", "sugar_cane", o, o, o, o, o, o);
        shapedRecipe2x2("book", Items.BOOK, 1, "paper", "paper", "paper", "leather");
        shapedRecipe2x2("writable_book", Items.WRITABLE_BOOK, 1, "book", "ink_sac", o, "feather");
        alias("book_and_quill", "writable_book");
        shapedRecipe3x3("bowl", Items.BOWL, 4, p, o, p, o, p, o, o, o, o);
        shapedRecipe2x2("blaze_powder", Items.BLAZE_POWDER, 2, "blaze_rod", o, o, o);
        shapedRecipe2x2("ender_eye", Items.ENDER_EYE, 1, "blaze_powder", "ender_pearl", o, o);
        alias("eye_of_ender", "ender_eye");
        shapedRecipe2x2("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1, "brown_mushroom", "sugar", o, "spider_eye");
        shapedRecipe3x3("fire_charge", Items.FIRE_CHARGE, 3, o, "blaze_powder", o, o, "coal", o, o, "gunpowder", o);
        shapedRecipe2x2("flower_banner_pattern", Items.FLOWER_BANNER_PATTERN, 1, "paper", "oxeye_daisy", o, o);
        simple("magma_cream", Items.MAGMA_CREAM, CollectMagmaCreamTask::new);
        // Slabs + Stairs + Walls
        shapedRecipeSlab("cobblestone_slab", Items.COBBLESTONE_SLAB, "cobblestone");
        shapedRecipeStairs("cobblestone_stairs", Items.COBBLESTONE_STAIRS, "cobblestone");
        shapedRecipeWall("cobblestone_wall", Items.COBBLESTONE_WALL, "cobblestone");
        shapedRecipeSlab("stone_slab", Items.STONE_SLAB, "stone");
        shapedRecipeStairs("stone_stairs", Items.STONE_STAIRS, "stone");
        shapedRecipeSlab("smooth_stone_slab", Items.SMOOTH_STONE_SLAB, "smooth_stone");
        shapedRecipeSlab("stone_brick_slab", Items.STONE_BRICK_SLAB, "stone_bricks");
        shapedRecipeStairs("stone_brick_stairs", Items.STONE_BRICK_STAIRS, "stone_bricks");
        shapedRecipeWall("stone_brick_wall", Items.STONE_BRICK_WALL, "stone_bricks");
        shapedRecipeSlab("mossy_stone_brick_slab", Items.MOSSY_STONE_BRICK_SLAB, "mossy_stone_bricks");
        shapedRecipeStairs("mossy_stone_brick_stairs", Items.MOSSY_STONE_BRICK_STAIRS, "mossy_stone_bricks");
        shapedRecipeWall("mossy_stone_brick_wall", Items.MOSSY_STONE_BRICK_WALL, "mossy_stone_bricks");
        shapedRecipeSlab("mossy_cobblestone_slab", Items.MOSSY_COBBLESTONE_SLAB, "mossy_cobblestone");
        shapedRecipeStairs("mossy_cobblestone_stairs", Items.MOSSY_COBBLESTONE_STAIRS, "mossy_cobblestone");
        shapedRecipeWall("mossy_cobblestone_wall", Items.MOSSY_COBBLESTONE_WALL, "mossy_cobblestone");
        shapedRecipeSlab("andesite_slab", Items.ANDESITE_SLAB, "andesite");
        shapedRecipeStairs("andesite_stairs", Items.ANDESITE_STAIRS, "andesite");
        shapedRecipeWall("andesite_wall", Items.ANDESITE_WALL, "andesite");
        shapedRecipeSlab("granite_slab", Items.GRANITE_SLAB, "granite");
        shapedRecipeStairs("granite_stairs", Items.GRANITE_STAIRS, "granite");
        shapedRecipeWall("granite_wall", Items.GRANITE_WALL, "granite");
        shapedRecipeSlab("diorite_slab", Items.DIORITE_SLAB, "diorite");
        shapedRecipeStairs("diorite_stairs", Items.DIORITE_STAIRS, "diorite");
        shapedRecipeWall("diorite_wall", Items.DIORITE_WALL, "diorite");
        shapedRecipeSlab("polished_andesite_slab", Items.POLISHED_ANDESITE_SLAB, "polished_andesite");
        shapedRecipeStairs("polished_andesite_stairs", Items.POLISHED_ANDESITE_STAIRS, "polished_andesite");
        shapedRecipeSlab("polished_granite_slab", Items.POLISHED_GRANITE_SLAB, "polished_granite");
        shapedRecipeStairs("polished_granite_stairs", Items.POLISHED_GRANITE_STAIRS, "polished_granite");
        shapedRecipeSlab("polished_diorite_slab", Items.POLISHED_DIORITE_SLAB, "polished_diorite");
        shapedRecipeStairs("polished_diorite_stairs", Items.POLISHED_DIORITE_STAIRS, "polished_diorite");
        shapedRecipeSlab("sandstone_slab", Items.SANDSTONE_SLAB, "sandstone");
        shapedRecipeStairs("sandstone_stairs", Items.SANDSTONE_STAIRS, "sandstone");
        shapedRecipeWall("sandstone_wall", Items.SANDSTONE_WALL, "sandstone");
        shapedRecipeSlab("cut_sandstone_slab", Items.CUT_SANDSTONE_SLAB, "cut_sandstone");
        shapedRecipeSlab("smooth_sandstone_slab", Items.SMOOTH_SANDSTONE_SLAB, "smooth_sandstone");
        shapedRecipeStairs("smooth_sandstone_stairs", Items.SMOOTH_SANDSTONE_STAIRS, "smooth_sandstone");
        shapedRecipeSlab("red_sandstone_slab", Items.RED_SANDSTONE_SLAB, "red_sandstone");
        shapedRecipeStairs("red_sandstone_stairs", Items.RED_SANDSTONE_STAIRS, "red_sandstone");
        shapedRecipeWall("red_sandstone_wall", Items.RED_SANDSTONE_WALL, "red_sandstone");
        shapedRecipeSlab("cut_red_sandstone_slab", Items.CUT_RED_SANDSTONE_SLAB, "cut_red_sandstone");
        shapedRecipeSlab("smooth_red_sandstone_slab", Items.SMOOTH_RED_SANDSTONE_SLAB, "smooth_red_sandstone");
        shapedRecipeStairs("smooth_red_sandstone_stairs", Items.SMOOTH_RED_SANDSTONE_STAIRS, "smooth_red_sandstone");
        shapedRecipeSlab("nether_brick_slab", Items.NETHER_BRICK_SLAB, "nether_bricks");
        shapedRecipeStairs("nether_brick_stairs", Items.NETHER_BRICK_STAIRS, "nether_bricks");
        shapedRecipeWall("nether_brick_wall", Items.NETHER_BRICK_WALL, "nether_bricks");
        shapedRecipeSlab("red_nether_brick_slab", Items.RED_NETHER_BRICK_SLAB, "red_nether_bricks");
        shapedRecipeStairs("red_nether_brick_stairs", Items.RED_NETHER_BRICK_STAIRS, "red_nether_bricks");
        shapedRecipeWall("red_nether_brick_wall", Items.RED_NETHER_BRICK_WALL, "red_nether_bricks");
        shapedRecipeSlab("quartz_slab", Items.QUARTZ_SLAB, "quartz_block");
        shapedRecipeStairs("quartz_stairs", Items.QUARTZ_STAIRS, "quartz_block");
        shapedRecipeSlab("smooth_quartz_slab", Items.SMOOTH_QUARTZ_SLAB, "smooth_quartz");
        shapedRecipeStairs("smooth_quartz_stairs", Items.SMOOTH_QUARTZ_STAIRS, "smooth_quartz");
        shapedRecipeSlab("blackstone_slab", Items.BLACKSTONE_SLAB, "blackstone");
        shapedRecipeStairs("blackstone_stairs", Items.BLACKSTONE_STAIRS, "blackstone");
        shapedRecipeWall("blackstone_wall", Items.BLACKSTONE_WALL, "blackstone");
        shapedRecipeSlab("polished_blackstone_slab", Items.POLISHED_BLACKSTONE_SLAB, "polished_blackstone");
        shapedRecipeStairs("polished_blackstone_stairs", Items.POLISHED_BLACKSTONE_STAIRS, "polished_blackstone");
        shapedRecipeWall("polished_blackstone_wall", Items.POLISHED_BLACKSTONE_WALL, "polished_blackstone");
        shapedRecipeSlab("polished_blackstone_brick_slab", Items.POLISHED_BLACKSTONE_BRICK_SLAB, "polished_blackstone_bricks");
        shapedRecipeStairs("polished_blackstone_brick_stairs", Items.POLISHED_BLACKSTONE_BRICK_STAIRS, "polished_blackstone_bricks");
        shapedRecipeWall("polished_blackstone_brick_wall", Items.POLISHED_BLACKSTONE_BRICK_WALL, "polished_blackstone_bricks");
        shapedRecipeSlab("cut_copper_slab", Items.CUT_COPPER_SLAB, "cut_copper");
        shapedRecipeStairs("cut_copper_stairs", Items.CUT_COPPER_STAIRS, "cut_copper");
        shapedRecipeSlab("cobbled_deepslate_slab", Items.COBBLED_DEEPSLATE_SLAB, "cobbled_deepslate");
        shapedRecipeStairs("cobbled_deepslate_stairs", Items.COBBLED_DEEPSLATE_STAIRS, "cobbled_deepslate");
        shapedRecipeWall("cobbled_deepslate_wall", Items.COBBLED_DEEPSLATE_WALL, "cobbled_deepslate");
        shapedRecipeSlab("polished_deepslate_slab", Items.POLISHED_DEEPSLATE_SLAB, "polished_deepslate");
        shapedRecipeStairs("polished_deepslate_stairs", Items.POLISHED_DEEPSLATE_STAIRS, "polished_deepslate");
        shapedRecipeWall("polished_deepslate_wall", Items.POLISHED_DEEPSLATE_WALL, "polished_deepslate");
        shapedRecipeSlab("deepslate_brick_slab", Items.DEEPSLATE_BRICK_SLAB, "deepslate_bricks");
        shapedRecipeStairs("deepslate_brick_stairs", Items.DEEPSLATE_BRICK_STAIRS, "deepslate_bricks");
        shapedRecipeWall("deepslate_brick_wall", Items.DEEPSLATE_BRICK_WALL, "deepslate_bricks");
        shapedRecipeSlab("deepslate_tile_slab", Items.DEEPSLATE_TILE_SLAB, "deepslate_tiles");
        shapedRecipeStairs("deepslate_tile_stairs", Items.DEEPSLATE_TILE_STAIRS, "deepslate_tiles");
        shapedRecipeWall("deepslate_tile_wall", Items.DEEPSLATE_TILE_WALL, "deepslate_tiles");
        shapedRecipe2x2("chiseled_sandstone", Items.CHISELED_SANDSTONE, 1, "sandstone_slab", o, "sandstone_slab", o);
        shapedRecipe2x2("chiseled_red_sandstone", Items.CHISELED_RED_SANDSTONE, 1, "red_sandstone_slab", o, "red_sandstone_slab", o);
        shapedRecipe2x2("chiseled_stone_bricks", Items.CHISELED_STONE_BRICKS, 1, "stone_brick_slab", o, "stone_brick_slab", o);
        shapedRecipe2x2("chiseled_nether_bricks", Items.CHISELED_NETHER_BRICKS, 1, "nether_brick_slab", o, "nether_brick_slab", o);
        shapedRecipe2x2("chiseled_quartz_block", Items.CHISELED_QUARTZ_BLOCK, 1, "quartz_slab", o, "quartz_slab", o);
        shapedRecipe2x2("chiseled_deepslate", Items.CHISELED_DEEPSLATE, 1, "cobbled_deepslate_slab", o, "cobbled_deepslate_slab", o);
    }
}
