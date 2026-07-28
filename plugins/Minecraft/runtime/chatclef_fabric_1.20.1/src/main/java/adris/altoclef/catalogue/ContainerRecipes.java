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
final class ContainerRecipes extends TaskRegistrar {
    private ContainerRecipes() {
    }

    static void register() {
        String p = "planks";
        String s = "stick";
        String o = null;

        // FURNITURE
        shapedRecipe2x2("crafting_table", Items.CRAFTING_TABLE, 1, p, p, p, p).dontMineIfPresent();
        shapedRecipe3x3("smithing_table", Items.SMITHING_TABLE, 1, "iron_ingot", "iron_ingot", o, p, p, o, p, p, o);
        shapedRecipe3x3("grindstone", Items.GRINDSTONE, 1, s, "stone_slab", s, p, o, p, o, o, o);
        simple("wooden_pressure_plate", ItemHelper.WOOD_PRESSURE_PLATE, CollectWoodenPressurePlateTask::new);
        woodTasks("pressure_plate", woodItems -> woodItems.pressurePlate, (woodItems, count) -> new CollectWoodenPressurePlateTask(woodItems.pressurePlate, woodItems.prefix + "_planks", count));
        simple("wooden_button", ItemHelper.WOOD_BUTTON, CollectWoodenButtonTask::new);
        woodTasks("button", woodItems -> woodItems.button, (woodItems, count) -> new CraftInInventoryTask(new RecipeTarget(woodItems.button, 1, CraftingRecipe.newShapedRecipe(woodItems.prefix + "_button", new ItemTarget[]{new ItemTarget(woodItems.planks, 1), null, null, null}, 1))));
        shapedRecipe2x2("stone_pressure_plate", Items.STONE_PRESSURE_PLATE, 1, o, o, "stone", "stone");
        shapedRecipe2x2("stone_button", Items.STONE_BUTTON, 1, "stone", o, o, o);
        shapedRecipe2x2("polished_blackstone_pressure_plate", Items.POLISHED_BLACKSTONE_PRESSURE_PLATE, 1, o, o, "polished_blackstone", "polished_blackstone");
        shapedRecipe2x2("polished_blackstone_button", Items.POLISHED_BLACKSTONE_BUTTON, 1, "polished_blackstone", o, o, o);
        simple("sign", ItemHelper.WOOD_SIGN, CollectSignTask::new).dontMineIfPresent(); // By default, we save signs round these parts.
        woodTasks("sign", woodItems -> woodItems.sign, (woodItems, count) -> new CollectSignTask(woodItems.sign, woodItems.prefix + "_planks", count));
        {
            String c = "cobblestone";
            shapedRecipe3x3("furnace", Items.FURNACE, 1, c, c, c, c, o, c, c, c, c).dontMineIfPresent();
            shapedRecipe3x3("dropper", Items.DROPPER, 1, c, c, c, c, o, c, c, "redstone", c);
            shapedRecipe3x3("dispenser", Items.DISPENSER, 1, c, c, c, c, "bow", c, c, "redstone", c);
            shapedRecipe3x3("brewing_stand", Items.BREWING_STAND, 1, o, o, o, o, "blaze_rod", o, c, c, c);
            shapedRecipe3x3("piston", Items.PISTON, 1, p, p, p, c, "iron_ingot", c, c, "redstone", c);
            shapedRecipe3x3("observer", Items.OBSERVER, 1, c, c, c, "redstone", "redstone", "quartz", c, c, c);
            shapedRecipe2x2("lever", Items.LEVER, 1, s, o, c, o);
        }
        simple("hanging_sign", ItemHelper.WOOD_HANGING_SIGN, CollectHangingSignTask::new).dontMineIfPresent();
        woodTasks("hanging_sign", woodItems -> woodItems.hangingSign, (woodItems, count) -> new CollectHangingSignTask(woodItems.hangingSign, woodItems.prefix + "_stripped_logs", count));
        shapedRecipe3x3("chest", Items.CHEST, 1, p, p, p, p, o, p, p, p, p).dontMineIfPresent();
        shapedRecipe2x2("torch", Items.TORCH, 4, "coal", o, s, o);
        simple("bed", ItemHelper.BED, CollectBedTask::new);
        colorfulTasks("bed", colors -> colors.bed, (colors, count) -> new CollectBedTask(colors.bed, colors.colorName + "_wool", count));
        {
            String i = "iron_ingot";
            String b = "iron_block";
            String m = "smooth_stone";
            shapedRecipe3x3("anvil", Items.ANVIL, 1, b, b, b, o, i, o, i, i, i);
            shapedRecipe3x3("cauldron", Items.CAULDRON, 1, i, o, i, i, o, i, i, i, i);
            shapedRecipe3x3("minecart", Items.MINECART, 1, o, o, o, i, o, i, i, i, i);
            shapedRecipe3x3("iron_door", Items.IRON_DOOR, 3, i, i, o, i, i, o, i, i, o);
            shapedRecipe3x3("iron_bars", Items.IRON_BARS, 16, i, i, i, i, i, i, o, o, o);
            shapedRecipe3x3("blast_furnace", Items.BLAST_FURNACE, 1, i, i, i, i, "furnace", i, m, m, m);
            shapedRecipe2x2Block("iron_trapdoor", Items.IRON_TRAPDOOR, i);
        }
        shapedRecipe3x3("armor_stand", Items.ARMOR_STAND, 1, s, s, s, o, s, o, s, "smooth_stone_slab", s);
        {
            String b = "obsidian";
            shapedRecipe3x3("enchanting_table", Items.ENCHANTING_TABLE, 1, o, "book", o, "diamond", b, "diamond", b, b, b);
            shapedRecipe3x3("ender_chest", Items.ENDER_CHEST, 1, b, b, b, b, "ender_eye", b, b, b, b).dontMineIfPresent();
        }
        {
            String b = "brick";
            shapedRecipe3x3("decorated_pot", Items.DECORATED_POT, 1, o, b, o, b, o, b, o, b, o);
            shapedRecipe3x3("flower_pot", Items.FLOWER_POT, 1, b, o, b, o, b, o, o, o, o);
            shapedRecipe2x2Block("bricks", Items.BRICKS, b);
            shapedRecipeSlab("brick_slab", Items.BRICK_SLAB, b);
            shapedRecipeStairs("brick_stairs", Items.BRICK_STAIRS, b);
            shapedRecipeStairs("brick_wall", Items.BRICK_WALL, "brick");
        }
        shapedRecipe3x3("ladder", Items.LADDER, 3, s, o, s, s, s, s, s, o, s);
        shapedRecipe3x3("jukebox", Items.JUKEBOX, 1, p, p, p, p, "diamond", p, p, p, p);
        shapedRecipe3x3("note_block", Items.NOTE_BLOCK, 1, p, p, p, p, "redstone", p, p, p, p);
        shapedRecipe3x3("redstone_lamp", Items.REDSTONE_LAMP, 1, o, "redstone", o, "redstone", "glowstone", "redstone", o, "redstone", o);
        shapedRecipe3x3("bookshelf", Items.BOOKSHELF, 1, p, p, p, "book", "book", "book", p, p, p);
        shapedRecipe2x2("loom", Items.LOOM, 1, "string", "string", p, p);
        {
            String g = "glass";
            shapedRecipe3x3("glass_pane", Items.GLASS_PANE, 16, g, g, g, g, g, g, o, o, o).dontMineIfPresent();
        }
        simple("carved_pumpkin", Items.CARVED_PUMPKIN, count -> new CarveThenCollectTask(Items.CARVED_PUMPKIN, count, Blocks.CARVED_PUMPKIN, Items.PUMPKIN, Blocks.PUMPKIN, Items.SHEARS));
        shapedRecipe2x2("jack_o_lantern", Items.JACK_O_LANTERN, 1, "carved_pumpkin", o, "torch", o);
        shapedRecipe3x3("target", Items.TARGET, 1, o, "redstone", o, "redstone", "hay_block", "redstone", o, "redstone", o);
        shapedRecipe3x3("campfire", Items.CAMPFIRE, 1, o, s, o, s, "coal", s, "log", "log", "log");
        shapedRecipe3x3("soul_campfire", Items.SOUL_CAMPFIRE, 1, o, s, o, s, "soul_soil", s, "log", "log", "log");
        shapedRecipe3x3("soul_torch", Items.SOUL_TORCH, 4, o, "coal", o, o, s, o, o, "soul_soil", o);
        {
            String l = "log";
            shapedRecipe3x3("smoker", Items.SMOKER, 1, o, l, o, l, "furnace", l, o, l, o).dontMineIfPresent();
        }
        {
            String i = "iron_nugget";
            shapedRecipe3x3("lantern", Items.LANTERN, 1, i, i, i, i, "torch", i, i, i, i);
            shapedRecipe3x3("soul_lantern", Items.SOUL_LANTERN, 1, i, i, i, i, "soul_torch", i, i, i, i);
            shapedRecipe3x3("chain", Items.CHAIN, 1, o, i, o, o, "iron_ingot", o, o, i, o);
        }
        {
            String c = "chiseled_stone_bricks";
            shapedRecipe3x3("lodestone", Items.LODESTONE, 1, c, c, c, c, "netherite_ingot", c, c, c, c);
        }
        shapedRecipe3x3("lightning_rod", Items.LIGHTNING_ROD, 1, o, "copper_ingot", o, o, "copper_ingot", o, o, "copper_ingot", o);
        shapedRecipe3x3("tinted_glass", Items.TINTED_GLASS, 2, o, "amethyst_shard", o, "amethyst_shard", "glass", "amethyst_shard", o, "amethyst_shard", o);

        // A BUNCH OF WOODEN STUFF
        simple("wooden_stairs", ItemHelper.WOOD_STAIRS, CollectWoodenStairsTask::new);
        woodTasks("stairs", woodItems -> woodItems.stairs, (woodItems, count) -> new CollectWoodenStairsTask(woodItems.stairs, woodItems.prefix + "_planks", count));
        simple("wooden_slab", ItemHelper.WOOD_SLAB, CollectWoodenSlabTask::new);
        woodTasks("slab", woodItems -> woodItems.slab, (woodItems, count) -> new CollectWoodenSlabTask(woodItems.slab, woodItems.prefix + "_planks", count));
        simple("wooden_door", ItemHelper.WOOD_DOOR, CollectWoodenDoorTask::new);
        woodTasks("door", woodItems -> woodItems.door, (woodItems, count) -> new CollectWoodenDoorTask(woodItems.door, woodItems.prefix + "_planks", count));
        simple("wooden_trapdoor", ItemHelper.WOOD_TRAPDOOR, CollectWoodenTrapDoorTask::new);
        woodTasks("trapdoor", woodItems -> woodItems.trapdoor, (woodItems, count) -> new CollectWoodenTrapDoorTask(woodItems.trapdoor, woodItems.prefix + "_planks", count));
        simple("wooden_fence", ItemHelper.WOOD_FENCE, CollectFenceTask::new);
        woodTasks("fence", woodItems -> woodItems.fence, (woodItems, count) -> new CollectFenceTask(woodItems.fence, woodItems.prefix + "_planks", count));
        simple("wooden_fence_gate", ItemHelper.WOOD_FENCE_GATE, CollectFenceGateTask::new);
        woodTasks("fence_gate", woodItems -> woodItems.fenceGate, (woodItems, count) -> new CollectFenceGateTask(woodItems.fenceGate, woodItems.prefix + "_planks", count));
        {
            String r = "wooden_slab";
            shapedRecipe3x3("chiseled_bookshelf", Items.CHISELED_BOOKSHELF, 1, p, p, p, r, r, r, p, p, p).dontMineIfPresent();
            shapedRecipe3x3("barrel", Items.BARREL, 1, p, r, p, p, o, p, p, r, p);
            shapedRecipe3x3("cartography_table", Items.CARTOGRAPHY_TABLE, 1, "paper", "paper", o, p, p, o, p, p, o);
            shapedRecipe3x3("composter", Items.COMPOSTER, 1, r, o, r, r, o, r, r, r, r);
            shapedRecipe3x3("fletching_table", Items.FLETCHING_TABLE, 1, "flint", "flint", o, p, p, o, p, p, o);
            shapedRecipe3x3("lectern", Items.LECTERN, 1, r, r, r, o, "bookshelf", o, o, r, o);
        }            // Most people will always think "wooden door" when they say "door".
        alias("door", "wooden_door");
        alias("trapdoor", "wooden_trapdoor");
        alias("fence", "wooden_fence");
        alias("fence_gate", "wooden_fence_gate");

        shapedRecipe2x2("heavy_weighted_pressure_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 1, "iron_ingot", "iron_ingot", o, o);
        shapedRecipe2x2("light_weighted_pressure_plate", Items.LIGHT_WEIGHTED_PRESSURE_PLATE, 1, "gold_ingot", "gold_ingot", o, o);

        shapedRecipe3x3("daylight_detector", Items.DAYLIGHT_DETECTOR, 1, "glass", "glass", "glass", "quartz", "quartz", "quartz", "wooden_slab", "wooden_slab", "wooden_slab");
        shapedRecipe3x3("tripwire_hook", Items.TRIPWIRE_HOOK, 2, "iron_ingot", o, o, "stick", o, o, "planks", o, o);
        shapedRecipe2x2("trapped_chest", Items.TRAPPED_CHEST, 1, "chest", "tripwire_hook", o, o);
        shapedRecipe3x3("crossbow", Items.CROSSBOW, 1, s, "iron_ingot", s, "string", "tripwire_hook", "string", o, s, o);
        {
            String t = "gunpowder";
            String n = "sand";
            shapedRecipe3x3("tnt", Items.TNT, 1, t, n, t, n, t, n, t, n, t);
        }
        shapedRecipe2x2("sticky_piston", Items.STICKY_PISTON, 1, "slime_ball", o, "piston", o);
        shapedRecipe2x2("redstone_torch", Items.REDSTONE_TORCH, 1, "redstone", o, s, o);
        shapedRecipe3x3("repeater", Items.REPEATER, 1, "redstone_torch", "redstone", "redstone_torch", "stone", "stone", "stone", o, o, o);
        shapedRecipe3x3("comparator", Items.COMPARATOR, 1, o, "redstone_torch", o, "redstone_torch", "quartz", "redstone_torch", "stone", "stone", "stone");
        {
            // Some rails
            String i = "iron_ingot";
            String g = "gold_ingot";
            shapedRecipe3x3("rail", Items.RAIL, 16, i, o, i, i, s, i, i, o, i);
            shapedRecipe3x3("powered_rail", Items.POWERED_RAIL, 6, g, o, g, g, s, g, g, "redstone", g);
            shapedRecipe3x3("detector_rail", Items.DETECTOR_RAIL, 6, i, o, i, i, "stone_pressure_plate", i, i, "redstone", i);
            shapedRecipe3x3("activator_rail", Items.ACTIVATOR_RAIL, 6, i, s, i, i, "redstone_torch", i, i, s, i);
            shapedRecipe3x3("hopper", Items.HOPPER, 1, i, o, i, i, "chest", i, o, i, o);
        }
        shapedRecipe3x3("painting", Items.PAINTING, 1, s, s, s, s, "wool", s, s, s, s);
        shapedRecipe3x3("item_frame", Items.ITEM_FRAME, 1, s, s, s, s, "leather", s, s, s, s);
        shapedRecipe2x2("glow_item_frame", Items.GLOW_ITEM_FRAME, 1, "item_frame", "glow_ink_sac", o, o);
        shapedRecipe2x2("chest_minecart", Items.CHEST_MINECART, 1, "chest", o, "minecart", o);
        shapedRecipe2x2("furnace_minecart", Items.FURNACE_MINECART, 1, "furnace", o, "minecart", o);
        shapedRecipe2x2("hopper_minecart", Items.HOPPER_MINECART, 1, "hopper", o, "minecart", o);
        shapedRecipe2x2("tnt_minecart", Items.TNT_MINECART, 1, "tnt", o, "minecart", o);
        alias("minecart_with_chest", "chest_minecart");
        alias("minecart_with_furnace", "furnace_minecart");
        alias("minecart_with_hopper", "hopper_minecart");
        alias("minecart_with_tnt", "tnt_minecart");
    }
}
