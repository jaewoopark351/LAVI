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
final class FoodRecipes extends TaskRegistrar {
    private FoodRecipes() {
    }

    static void register() {
        String p = "planks";
        String s = "stick";
        String o = null;

        /// FOOD
        mobCook("porkchop", Items.PORKCHOP, Items.COOKED_PORKCHOP, PigEntity.class);
        mobCook("beef", Items.BEEF, Items.COOKED_BEEF, CowEntity.class);
        mobCook("chicken", Items.CHICKEN, Items.COOKED_CHICKEN, ChickenEntity.class);
        mobCook("mutton", Items.MUTTON, Items.COOKED_MUTTON, SheepEntity.class);
        mobCook("rabbit", Items.RABBIT, Items.COOKED_RABBIT, RabbitEntity.class);
        mobCook("salmon", Items.SALMON, Items.COOKED_SALMON, SalmonEntity.class);
        mobCook("cod", Items.COD, Items.COOKED_COD, CodEntity.class);
        simple("milk", Items.MILK_BUCKET, CollectMilkTask::new);
        mine("apple", Blocks.OAK_LEAVES, Items.APPLE);
        smelt("baked_potato", Items.BAKED_POTATO, "potato");
        shapedRecipe2x2("mushroom_stew", Items.MUSHROOM_STEW, 1, "red_mushroom", "brown_mushroom", "bowl", o);
        shapedRecipe2x2("suspicious_stew", Items.SUSPICIOUS_STEW, 1, "red_mushroom", "brown_mushroom", "bowl", "flower");
        shapedRecipe3x3("bread", Items.BREAD, 1, "wheat", "wheat", "wheat", o, o, o, o, o, o);
        shapedRecipe3x3("cookie", Items.COOKIE, 8, "wheat", "cocoa_beans", "wheat", o, o, o, o, o, o);
        shapedRecipe2x2("pumpkin_pie", Items.PUMPKIN_PIE, 1, "pumpkin", "sugar", o, "egg");
        shapedRecipe3x3("cake", Items.CAKE, 1, "milk", "milk", "milk", "sugar", "egg", "sugar", "wheat", "wheat", "wheat").dontMineIfPresent();
        {
            String g = "gold_nugget";
            shapedRecipe3x3("golden_carrot", Items.GOLDEN_CARROT, 1, g, g, g, g, "carrot", g, g, g, g);
            String i = "gold_ingot";
            shapedRecipe3x3("golden_apple", Items.GOLDEN_APPLE, 1, i, i, i, i, "apple", i, i, i, i);
        }
        shapedRecipe3x3("rabbit_stew", Items.RABBIT_STEW, 1, o, "cooked_rabbit", o, "carrot", "baked_potato", "mushroom", o, "bowl", o);
        {
            String b = "beetroot";
            shapedRecipe3x3("beetroot_soup", Items.BEETROOT_SOUP, 1, b, b, b, b, b, b, o, "bowl", o);
        }
    }
}
