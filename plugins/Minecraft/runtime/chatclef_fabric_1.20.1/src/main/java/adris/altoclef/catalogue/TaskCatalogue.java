package adris.altoclef.catalogue;

import adris.altoclef.Debug;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Entities;
import adris.altoclef.multiversion.versionedfields.Items;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasks.container.UpgradeInSmithingTableTask;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.tasks.resources.food.CollectFoodTask;
import adris.altoclef.tasks.resources.wood.*;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.util.DyeColor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Contains a hardcoded list of ALL obtainable resources.
 * <p>
 * Most resources correspond to a single item, but some resources (like "log" or "door") include a range of items.
 * <p>
 * Call `TaskCatalogue.getItemTask` to return a task given a resource key.
 * Call `TaskCatalogue.getSquashedItemTask` to return a task that gets multiple resources, combining their steps.
 */
@SuppressWarnings({"rawtypes"})
public class TaskCatalogue {

    private static final HashMap<String, Item[]> nameToItemMatches = new HashMap<>();
    private static final HashMap<String, CataloguedResource> nameToResourceTask = new HashMap<>();
    private static final HashMap<Item, CataloguedResource> itemToResourceTask = new HashMap<>();
    private static final HashSet<Item> resourcesObtainable = new HashSet<>();

    static {
        /// DEFINE RESOURCE TASKS HERE
        BlockResources.register();
        MaterialRecipes.register();
        ToolRecipes.register();
        ContainerRecipes.register();
        FoodRecipes.register();
    }

    static CataloguedResource put(String name, Item[] matches, Function<Integer, ResourceTask> getTask) {
        List<Item> supportedMatches = new ArrayList<>();
        for (Item item : matches) {
            if (item != Items.UNSUPPORTED) {
                supportedMatches.add(item);
            }
        }
        matches = supportedMatches.toArray(new Item[0]);

        CataloguedResource result = new CataloguedResource(matches, getTask);
        Block[] blocks = ItemHelper.itemsToBlocks(matches);
        // DEFAULT BEHAVIOUR: Mine if present & assume overworld is required!
        if (blocks.length != 0) {
            result.mineIfPresent();
        }
        result.forceDimension(Dimension.OVERWORLD);
        if (nameToResourceTask.containsKey(name)) {
            throw new IllegalStateException("Tried cataloguing " + name + " twice!");
        }
        nameToResourceTask.put(name, result);
        nameToItemMatches.put(name, matches);
        resourcesObtainable.addAll(Arrays.asList(matches));

        // If this resource is just one item, consider it collectable.
        if (matches.length == 1) {
            if (itemToResourceTask.containsKey(matches[0])) {
                throw new IllegalStateException("Tried cataloguing " + matches[0].getTranslationKey() + " twice!");
            }
            itemToResourceTask.put(matches[0], result);
        }

        return result;
    }

    // This is here so that we can use strings for item targets (optionally) and stuff like that.
    public static Item[] getItemMatches(String name) {
        if (!nameToItemMatches.containsKey(name)) {
            return new Item[0];
        }
        return nameToItemMatches.get(name);
    }

    public static boolean isObtainable(Item item) {
        return resourcesObtainable.contains(item);
    }

    public static ItemTarget getItemTarget(String name, int count) {
        return new ItemTarget(name, count);
    }

    public static CataloguedResourceTask getSquashedItemTask(ItemTarget... targets) {
        return new CataloguedResourceTask(true, targets);
    }

    public static ResourceTask getItemTask(String name, int count) {

        if (!taskExists(name)) {
            Debug.logWarning("Task " + name + " does not exist. Error possibly.");
            Debug.logStack();
            return null;
        }

        return nameToResourceTask.get(name).getResource(count);
    }

    public static ResourceTask getItemTask(Item item, int count) {
        if (!taskExists(item)) {
            Debug.logWarning("Task " + item + " does not exist. Error possibly.");
            Debug.logStack();
            return null;
        }

        return itemToResourceTask.get(item).getResource(count);
    }

    public static ResourceTask getItemTask(ItemTarget target) {
        if (target.isCatalogueItem()) {
            return getItemTask(target.getCatalogueName(), target.getTargetCount());
        } else if (target.getMatches().length == 1) {
            return getItemTask(target.getMatches()[0], target.getTargetCount());
        } else {
            return getSquashedItemTask(target);
        }
    }

    public static boolean taskExists(String name) {
        return nameToResourceTask.containsKey(name);
    }

    public static boolean taskExists(Item item) {
        return itemToResourceTask.containsKey(item);
    }

    public static Collection<String> resourceNames() {
        return nameToResourceTask.keySet();
    }

    static CataloguedResource simple(String name, Item[] matches, Function<Integer, ResourceTask> getTask) {
        return put(name, matches, getTask);
    }

    static CataloguedResource simple(String name, Item matches, Function<Integer, ResourceTask> getTask) {
        return simple(name, new Item[]{matches}, getTask);
    }

    // TODO: Do I really need this?
    //private static CataloguedResource task(Item matches, Function<Integer, ResourceTask> getTask){
    //
    //}

    static CataloguedResource mine(String name, MiningRequirement requirement, Item[] toMine, Item... targets) {
        Block[] toMineBlocks = new Block[toMine.length];
        for (int i = 0; i < toMine.length; ++i) toMineBlocks[i] = Block.getBlockFromItem(toMine[i]);
        return mine(name, requirement, toMineBlocks, targets);
    }

    static CataloguedResource mine(String name, MiningRequirement requirement, Block[] toMine, Item... targets) {
        return put(name, targets, count -> new MineAndCollectTask(new ItemTarget(targets, count), toMine, requirement)).dontMineIfPresent(); // Mining already taken care of!!
    }

    static CataloguedResource mine(String name, MiningRequirement requirement, Block toMine, Item target) {
        return mine(name, requirement, new Block[]{toMine}, target);
    }

    static CataloguedResource mine(String name, Block toMine, Item target) {
        return mine(name, MiningRequirement.HAND, toMine, target);
    }

    static CataloguedResource mine(String name, Item target) {
        return mine(name, Block.getBlockFromItem(target), target);
    }

    static CataloguedResource shear(String name, Block[] toShear, Item... targets) {
        return put(name, targets, count -> new ShearAndCollectBlockTask(new ItemTarget[]{new ItemTarget(targets, count)}, toShear)).dontMineIfPresent();
    }

    static CataloguedResource shear(String name, Block toShear, Item... targets) {
        return shear(name, new Block[]{toShear}, targets);
    }

    static CataloguedResource shapedRecipe2x2(String name, Item match, int outputCount, String s0, String s1, String s2, String s3) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[]{t(s0), t(s1), t(s2), t(s3)}, outputCount);
        return put(name, new Item[]{match}, count -> new CraftInInventoryTask(new RecipeTarget(match, count, recipe)));
    }

    static CataloguedResource shapedRecipe3x3(String name, Item match, int outputCount, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[]{t(s0), t(s1), t(s2), t(s3), t(s4), t(s5), t(s6), t(s7), t(s8)}, outputCount);
        return put(name, new Item[]{match}, count -> new CraftInTableTask(new RecipeTarget(match, count, recipe)));
    }

    static CataloguedResource shapedRecipe2x2Block(String name, Item match, String material) {
        return shapedRecipe2x2(name, match, 1, material, material, material, material);
    }

    static CataloguedResource shapedRecipe2x2Block(String name, Item match, int outputCount, String material) {
        return shapedRecipe2x2(name, match, outputCount, material, material, material, material);
    }

    static CataloguedResource shapedRecipe3x3Block(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 1, material, material, material, material, material, material, material, material, material);
    }

    static CataloguedResource shapedRecipeSlab(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 6, null, null, null, null, null, null, material, material, material);
    }

    static CataloguedResource shapedRecipeStairs(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 4, material, null, null, material, material, null, material, material, material);
    }

    static CataloguedResource shapedRecipeWall(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 6, material, material, material, material, material, material, null, null, null);
    }

    static CataloguedResource smelt(String name, Item[] matches, String materials, Item... optionalMaterials) {
        return put(name, matches, count -> new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(matches, count), new ItemTarget(materials, count), optionalMaterials)));
    }

    static CataloguedResource smelt(String name, Item match, String materials, Item... optionalMaterials) {
        return smelt(name, new Item[]{match}, materials, optionalMaterials);
    }

    static CataloguedResource smith(String name, Item[] matches, String materials, String tool) {
        return put(name, matches, count -> new UpgradeInSmithingTableTask(new ItemTarget(tool, count), new ItemTarget(materials, count), new ItemTarget(matches, count)));//new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(matches, count), new ItemTarget(materials, count))));
    }

    static CataloguedResource smith(String name, Item match, String materials, String tool) {
        return smith(name, new Item[]{match}, materials, tool);
    }

    static CataloguedResource mob(String name, Item[] matches, Class mobClass) {
        return put(name, matches, count -> new KillAndLootTask(mobClass, new ItemTarget(matches, count)));
    }

    static CataloguedResource mob(String name, Item match, Class mobClass) {
        return mob(name, new Item[]{match}, mobClass);
    }

    static void mobCook(String uncookedName, String cookedName, Item uncooked, Item cooked, Class mobClass) {
        mob(uncookedName, uncooked, mobClass);
        smelt(cookedName, cooked, uncookedName);
    }

    static void mobCook(String uncookedName, Item uncooked, Item cooked, Class mobClass) {
        mobCook(uncookedName, "cooked_" + uncookedName, uncooked, cooked, mobClass);
    }

    static CataloguedResource crop(String name, Item[] matches, Block[] cropBlocks, Item[] cropSeeds) {
        return put(name, matches, count -> new CollectCropTask(new ItemTarget(matches, count), cropBlocks, cropSeeds));
    }

    public static CataloguedResource crop(String name, Item match, Block cropBlock, Item cropSeed) {
        return crop(name, new Item[]{match}, new Block[]{cropBlock}, new Item[]{cropSeed});
    }

    static void colorfulTasks(String baseName, Function<ItemHelper.ColorfulItems, Item> getMatch, BiFunction<ItemHelper.ColorfulItems, Integer, ResourceTask> getTask) {
        for (DyeColor dCol : DyeColor.values()) {
            MapColor mCol = dCol.getMapColor();
            ItemHelper.ColorfulItems color = ItemHelper.getColorfulItems(mCol);
            String prefix = color.colorName;
            put(prefix + "_" + baseName, new Item[]{getMatch.apply(color)}, count -> getTask.apply(color, count));
        }
    }

    static CataloguedResource[] woodTasks(Function<ItemHelper.WoodItems, String> getCatalogueName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask, boolean requireNetherForNetherStuff) {
        List<CataloguedResource> result = new ArrayList<>();
        for (WoodType woodType : WoodType.values()) {
            ItemHelper.WoodItems woodItems = ItemHelper.getWoodItems(woodType);
            Item match = getMatch.apply(woodItems);
            String cataloguedName = getCatalogueName.apply(woodItems);
            if (match == null) continue;
            boolean isNether = woodItems.isNetherWood();
            CataloguedResource t = put(cataloguedName, new Item[]{match}, count -> getTask.apply(woodItems, count));
            if (requireNetherForNetherStuff && isNether) {
                t.forceDimension(Dimension.NETHER);
            }
            result.add(t);
        }
        return result.toArray(CataloguedResource[]::new);
    }

    static CataloguedResource[] woodTasks(String baseName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask, boolean requireNetherForNetherStuff) {
        return woodTasks(woodItem -> woodItem.prefix + "_" + baseName, getMatch, getTask, requireNetherForNetherStuff);
    }

    static CataloguedResource[] woodTasks(String baseName, Function<ItemHelper.WoodItems, Item> getMatch, BiFunction<ItemHelper.WoodItems, Integer, ResourceTask> getTask) {
        return woodTasks(baseName, getMatch, getTask, false);
    }

    static void tools(String toolMaterialName, String material, Item pickaxeItem, Item shovelItem, Item swordItem, Item axeItem, Item hoeItem) {
        String s = "stick";
        String o = null;
        //noinspection UnnecessaryLocalVariable
        String m = material;
        shapedRecipe3x3(toolMaterialName + "_pickaxe", pickaxeItem, 1, m, m, m, o, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_shovel", shovelItem, 1, o, m, o, o, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_sword", swordItem, 1, o, m, o, o, m, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_axe", axeItem, 1, m, m, o, m, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_hoe", hoeItem, 1, m, m, o, o, s, o, o, s, o);
    }

    static void armor(String armorMaterialName, String material, Item helmetItem, Item chestplateItem, Item leggingsItem, Item bootsItem) {
        String o = null;
        //noinspection UnnecessaryLocalVariable
        String m = material;
        shapedRecipe3x3(armorMaterialName + "_helmet", helmetItem, 1, m, m, m, m, o, m, o, o, o);
        shapedRecipe3x3(armorMaterialName + "_chestplate", chestplateItem, 1, m, o, m, m, m, m, m, m, m);
        shapedRecipe3x3(armorMaterialName + "_leggings", leggingsItem, 1, m, m, m, m, o, m, m, o, m);
        shapedRecipe3x3(armorMaterialName + "_boots", bootsItem, 1, o, o, o, m, o, m, m, o, m);
    }

    static void alias(String newName, String original) {
        if (!nameToResourceTask.containsKey(original) || !nameToItemMatches.containsKey(original)) {
            Debug.logWarning("Invalid resource: " + original + ". Will not create alias.");
        } else {
            nameToResourceTask.put(newName, nameToResourceTask.get(original));
            nameToItemMatches.put(newName, nameToItemMatches.get(original));
        }
    }

    static ItemTarget t(String cataloguedName) {
        return new ItemTarget(cataloguedName);
    }

    static class CataloguedResource {
        private final Item[] _targets;
        private final Function<Integer, ResourceTask> _getResource;

        private boolean _mineIfPresent;
        private boolean _forceDimension = false;
        private Dimension _targetDimension;

        public CataloguedResource(Item[] targets, Function<Integer, ResourceTask> getResource) {
            _targets = targets;
            _getResource = getResource;
        }

        public CataloguedResource mineIfPresent() {
            _mineIfPresent = true;
            return this;
        }

        public CataloguedResource dontMineIfPresent() {
            _mineIfPresent = false;
            return this;
        }

        public CataloguedResource forceDimension(Dimension dimension) {
            _forceDimension = true;
            _targetDimension = dimension;
            return this;
        }

        public CataloguedResource anyDimension() {
            _forceDimension = false;
            return this;
        }

        public ResourceTask getResource(int count) {
            ResourceTask result = _getResource.apply(count);
            if (_mineIfPresent) {
                result = result.mineIfPresent(ItemHelper.itemsToBlocks(_targets));
            }
            if (_forceDimension) {
                result = result.forceDimension(_targetDimension);
            }
            return result;
        }
    }
}
