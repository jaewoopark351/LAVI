package adris.altoclef.catalogue;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.function.BiFunction;
import java.util.function.Function;

//20260728_kpopmodder: Shared facade for catalogue registrar classes so TaskCatalogue owns storage only.
abstract class TaskRegistrar {
    protected static TaskCatalogue.CataloguedResource simple(String name, Item[] matches, Function<Integer, ResourceTask> getTask) {
        return TaskCatalogue.simple(name, matches, getTask);
    }

    protected static TaskCatalogue.CataloguedResource simple(String name, Item matches, Function<Integer, ResourceTask> getTask) {
        return TaskCatalogue.simple(name, matches, getTask);
    }

    protected static TaskCatalogue.CataloguedResource mine(String name, MiningRequirement requirement, Item[] toMine, Item... targets) {
        return TaskCatalogue.mine(name, requirement, toMine, targets);
    }

    protected static TaskCatalogue.CataloguedResource mine(String name, MiningRequirement requirement, Block[] toMine, Item... targets) {
        return TaskCatalogue.mine(name, requirement, toMine, targets);
    }

    protected static TaskCatalogue.CataloguedResource mine(String name, MiningRequirement requirement, Block toMine, Item target) {
        return TaskCatalogue.mine(name, requirement, toMine, target);
    }

    protected static TaskCatalogue.CataloguedResource mine(String name, Block toMine, Item target) {
        return TaskCatalogue.mine(name, toMine, target);
    }

    protected static TaskCatalogue.CataloguedResource mine(String name, Item target) {
        return TaskCatalogue.mine(name, target);
    }

    protected static TaskCatalogue.CataloguedResource shear(String name, Block[] toShear, Item... targets) {
        return TaskCatalogue.shear(name, toShear, targets);
    }

    protected static TaskCatalogue.CataloguedResource shear(String name, Block toShear, Item... targets) {
        return TaskCatalogue.shear(name, toShear, targets);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipe2x2(String name, Item match, int outputCount, String s0, String s1, String s2, String s3) {
        return TaskCatalogue.shapedRecipe2x2(name, match, outputCount, s0, s1, s2, s3);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipe3x3(String name, Item match, int outputCount, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        return TaskCatalogue.shapedRecipe3x3(name, match, outputCount, s0, s1, s2, s3, s4, s5, s6, s7, s8);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipe2x2Block(String name, Item match, String material) {
        return TaskCatalogue.shapedRecipe2x2Block(name, match, material);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipe2x2Block(String name, Item match, int outputCount, String material) {
        return TaskCatalogue.shapedRecipe2x2Block(name, match, outputCount, material);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipe3x3Block(String name, Item match, String material) {
        return TaskCatalogue.shapedRecipe3x3Block(name, match, material);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipeSlab(String name, Item match, String material) {
        return TaskCatalogue.shapedRecipeSlab(name, match, material);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipeStairs(String name, Item match, String material) {
        return TaskCatalogue.shapedRecipeStairs(name, match, material);
    }

    protected static TaskCatalogue.CataloguedResource shapedRecipeWall(String name, Item match, String material) {
        return TaskCatalogue.shapedRecipeWall(name, match, material);
    }

    protected static TaskCatalogue.CataloguedResource smelt(String name, Item[] matches, String materials, Item... optionalMaterials) {
        return TaskCatalogue.smelt(name, matches, materials, optionalMaterials);
    }

    protected static TaskCatalogue.CataloguedResource smelt(String name, Item match, String materials, Item... optionalMaterials) {
        return TaskCatalogue.smelt(name, match, materials, optionalMaterials);
    }

    protected static TaskCatalogue.CataloguedResource smith(String name, Item[] matches, String materials, String tool) {
        return TaskCatalogue.smith(name, matches, materials, tool);
    }

    protected static TaskCatalogue.CataloguedResource smith(String name, Item match, String materials, String tool) {
        return TaskCatalogue.smith(name, match, materials, tool);
    }

    protected static TaskCatalogue.CataloguedResource mob(String name, Item[] matches, Class mobClass) {
        return TaskCatalogue.mob(name, matches, mobClass);
    }

    protected static TaskCatalogue.CataloguedResource mob(String name, Item match, Class mobClass) {
        return TaskCatalogue.mob(name, match, mobClass);
    }

    protected static void mobCook(String uncookedName, String cookedName, Item uncooked, Item cooked, Class mobClass) {
        TaskCatalogue.mobCook(uncookedName, cookedName, uncooked, cooked, mobClass);
    }

    protected static void mobCook(String uncookedName, Item uncooked, Item cooked, Class mobClass) {
        TaskCatalogue.mobCook(uncookedName, uncooked, cooked, mobClass);
    }

    protected static TaskCatalogue.CataloguedResource crop(String name, Item[] matches, Block[] cropBlocks, Item[] cropSeeds) {
        return TaskCatalogue.crop(name, matches, cropBlocks, cropSeeds);
    }

    protected static TaskCatalogue.CataloguedResource crop(String name, Item match, Block cropBlock, Item cropSeed) {
        return TaskCatalogue.crop(name, match, cropBlock, cropSeed);
    }

    protected static void colorfulTasks(String baseName, Function<ItemHelper.ColorfulItems, Item> getMatch, BiFunction<ItemHelper.ColorfulItems, Integer, ResourceTask> getTask) {
        TaskCatalogue.colorfulTasks(baseName, getMatch, getTask);
    }

    protected static TaskCatalogue.CataloguedResource[] woodTasks(Function<ItemHelper.WoodItems, String> getCatalogueName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask, boolean requireNetherForNetherStuff) {
        return TaskCatalogue.woodTasks(getCatalogueName, getMatch, getTask, requireNetherForNetherStuff);
    }

    protected static TaskCatalogue.CataloguedResource[] woodTasks(String baseName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask, boolean requireNetherForNetherStuff) {
        return TaskCatalogue.woodTasks(baseName, getMatch, getTask, requireNetherForNetherStuff);
    }

    protected static TaskCatalogue.CataloguedResource[] woodTasks(String baseName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask) {
        return TaskCatalogue.woodTasks(baseName, getMatch, getTask);
    }

    protected static void tools(String toolMaterialName, String material, Item pickaxeItem, Item shovelItem, Item swordItem, Item axeItem, Item hoeItem) {
        TaskCatalogue.tools(toolMaterialName, material, pickaxeItem, shovelItem, swordItem, axeItem, hoeItem);
    }

    protected static void armor(String armorMaterialName, String material, Item helmetItem, Item chestplateItem, Item leggingsItem, Item bootsItem) {
        TaskCatalogue.armor(armorMaterialName, material, helmetItem, chestplateItem, leggingsItem, bootsItem);
    }

    protected static void alias(String newName, String original) {
        TaskCatalogue.alias(newName, original);
    }

    protected static ItemTarget t(String cataloguedName) {
        return TaskCatalogue.t(cataloguedName);
    }
}
