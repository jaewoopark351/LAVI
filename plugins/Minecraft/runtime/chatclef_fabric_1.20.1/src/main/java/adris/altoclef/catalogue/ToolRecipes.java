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
final class ToolRecipes extends TaskRegistrar {
    private ToolRecipes() {
    }

    static void register() {
        String p = "planks";
        String s = "stick";
        String o = null;

        /// TOOLS
        tools("wooden", "planks", Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_HOE);
        tools("stone", "cobblestone", Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_HOE);
        tools("iron", "iron_ingot", Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HOE);
        tools("golden", "gold_ingot", Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_HOE);
        tools("diamond", "diamond", Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_HOE);
        armor("leather", "leather", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
        armor("iron", "iron_ingot", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        armor("golden", "gold_ingot", Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        armor("diamond", "diamond", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        smith("netherite_helmet", Items.NETHERITE_HELMET, "netherite_ingot", "diamond_helmet");
        smith("netherite_chestplate", Items.NETHERITE_CHESTPLATE, "netherite_ingot", "diamond_chestplate");
        smith("netherite_leggings", Items.NETHERITE_LEGGINGS, "netherite_ingot", "diamond_leggings");
        smith("netherite_boots", Items.NETHERITE_BOOTS, "netherite_ingot", "diamond_boots");
        smith("netherite_pickaxe", Items.NETHERITE_PICKAXE, "netherite_ingot", "diamond_pickaxe");
        smith("netherite_axe", Items.NETHERITE_AXE, "netherite_ingot", "diamond_axe");
        smith("netherite_shovel", Items.NETHERITE_SHOVEL, "netherite_ingot", "diamond_shovel");
        smith("netherite_sword", Items.NETHERITE_SWORD, "netherite_ingot", "diamond_sword");
        smith("netherite_hoe", Items.NETHERITE_HOE, "netherite_ingot", "diamond_hoe");
        shapedRecipe3x3("bow", Items.BOW, 1, "string", s, o, "string", o, s, "string", s, o);
        shapedRecipe3x3("arrow", Items.ARROW, 4, "flint", o, o, s, o, o, "feather", o, o);
        {
            String i = "iron_ingot";
            shapedRecipe3x3("bucket", Items.BUCKET, 1, i, o, i, o, i, o, o, o, o);
            shapedRecipe2x2("flint_and_steel", Items.FLINT_AND_STEEL, 1, i, o, o, "flint");
            shapedRecipe2x2("shears", Items.SHEARS, 1, i, o, o, i);
            shapedRecipe2x2("iron_nugget", Items.IRON_NUGGET, 9, i, o, o, o);
            shapedRecipe3x3("compass", Items.COMPASS, 1, o, i, o, i, "redstone", i, o, i, o);
            shapedRecipe3x3("shield", Items.SHIELD, 1, p, i, p, p, p, p, o, p, o);
            String g = "gold_ingot";
            shapedRecipe3x3("clock", Items.CLOCK, 1, o, g, o, g, "redstone", g, o, g, o);
        }
        simple("water_bucket", Items.WATER_BUCKET, CollectBucketLiquidTask.CollectWaterBucketTask::new);
        simple("lava_bucket", Items.LAVA_BUCKET, CollectBucketLiquidTask.CollectLavaBucketTask::new);
        {
            String a = "paper";
            shapedRecipe3x3("map", Items.MAP, 1, a, a, a, a, "compass", a, a, a, a);
        }
        shapedRecipe3x3("fishing_rod", Items.FISHING_ROD, 1, o, o, s, o, s, "string", s, o, "string");
        shapedRecipe2x2("carrot_on_a_stick", Items.CARROT_ON_A_STICK, 1, "fishing_rod", "carrot", o, o);
        shapedRecipe2x2("warped_fungus_on_a_stick", Items.WARPED_FUNGUS_ON_A_STICK, 1, "fishing_rod", "warped_fungus", o, o);
        shapedRecipe3x3("spyglass", Items.SPYGLASS, 1, o, "amethyst_shard", o, o, "copper_ingot", o, o, "copper_ingot", o);
        shapedRecipe3x3("glass_bottle", Items.GLASS_BOTTLE, 3, "glass", o, "glass", o, "glass", o, o, o, o);
        {
            String l = "leather";
            shapedRecipe3x3("leather_horse_armor", Items.LEATHER_HORSE_ARMOR, 1, l, o, l, l, l, l, l, o, l);
        }
        alias("wooden_pick", "wooden_pickaxe");
        alias("stone_pick", "stone_pickaxe");
        alias("iron_pick", "iron_pickaxe");
        alias("gold_pick", "golden_pickaxe");
        alias("diamond_pick", "diamond_pickaxe");
        alias("netherite_pick", "netherite_pickaxe");
        simple("boat", ItemHelper.WOOD_BOAT, CollectBoatTask::new);
        woodTasks("boat", woodItems -> woodItems.boat, (woodItems, count) -> new CollectBoatTask(woodItems.boat, woodItems.prefix + "_planks", count));
        shapedRecipe3x3("lead", Items.LEAD, 1, "string", "string", o, "string", "slime_ball", o, o, o, "string");

        simple("honeycomb", Items.HONEYCOMB, CollectHoneycombTask::new);
        {
            String h = "honeycomb";
            shapedRecipe2x2Block("honeycomb_block", Items.HONEYCOMB_BLOCK, h);
            shapedRecipe2x2("candle", Items.CANDLE, 1, "string", o, h, o);
            shapedRecipe3x3("beehive", Items.BEEHIVE, 1, p, p, p, h, h, h, p, p, p);
        }
    }
}
