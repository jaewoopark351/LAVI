package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.item.Item;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

// Collects everything that's catalogued for a recipe.
public class CollectRecipeCataloguedResourcesTask extends Task {

    private final RecipeTarget[] _targets;
    private final boolean _ignoreUncataloguedSlots;
    //20260729_kpopmodder: Show which recipe material request caused a container-crafting detour.
    private final StateChangeLogger debugLogger = new StateChangeLogger("CollectRecipeCataloguedResourcesTask");
    private boolean _finished = false;
    private String lastPlanEventKey = "";

    public CollectRecipeCataloguedResourcesTask(boolean ignoreUncataloguedSlots, RecipeTarget... targets) {
        _targets = targets;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    @Override
    protected void onStart() {
        _finished = false;
        lastPlanEventKey = "";
        debugLogger.event("start diagnostics: targets=" + describeTargets()
                + ", ignoreUncataloguedSlots=" + _ignoreUncataloguedSlots);
    }

    @Override
    protected Task onTick() {
        // TODO: Cache this once instead of doing it every frame.
        AltoClef mod = AltoClef.getInstance();

        // Stuff to get, both catalogued + individual items.
        HashMap<String, Integer> catalogueCount = new HashMap<>();
        HashMap<Item, Integer> itemCount = new HashMap<>();

        for (RecipeTarget target : _targets) {
            // Ignore this recipe if we have its item.
            //if (mod.getItemStorage().targetMet(target.getItem())) continue;

            // null = empty which is always met.
            if (target == null) continue;

            int weNeed = target.getTargetCount() - mod.getItemStorage().getItemCount(target.getOutputItem());

            if (weNeed > 0) {
                CraftingRecipe recipe = target.getRecipe();
                // Default, just go through the recipe slots and collect the first one.
                for (int i = 0; i < recipe.getSlotCount(); ++i) {
                    ItemTarget slot = recipe.getSlot(i);
                    if (slot == null || slot.isEmpty()) continue;
                    int numberOfRepeats = (int) Math.floor(-0.1 + (double) weNeed / target.getRecipe().outputCount()) + 1;
                    if (!slot.isCatalogueItem()) {
                        if (slot.getMatches().length != 1) {
                            if (!_ignoreUncataloguedSlots) {
                                Debug.logWarning("Recipe collection for recipe " + recipe + " slot " + i
                                        + " is not catalogued. Please define an explicit"
                                        + " collectRecipeSubTask() function for this item target:" + slot
                                );
                            }
                        } else {
                            Item item = slot.getMatches()[0];
                            itemCount.put(item, itemCount.getOrDefault(item, 0) + numberOfRepeats);
                        }
                    } else {
                        String targetName = slot.getCatalogueName();
                        // How many "repeats" of a recipe we will need.
                        catalogueCount.put(targetName, catalogueCount.getOrDefault(targetName, 0) + numberOfRepeats);
                    }
                }
            }
        }


        // (Cache this with the above stuff!!)
        // Grab materials
        for (String catalogueMaterialName : catalogueCount.keySet()) {
            int count = catalogueCount.get(catalogueMaterialName);
            if (count > 0) {
                ItemTarget itemTarget = new ItemTarget(catalogueMaterialName, count);
                if (!StorageHelper.itemTargetsMet(mod, itemTarget)) {
                    setDebugState("Getting " + itemTarget);
                    logPlanEvent("catalogue:" + catalogueMaterialName + ":" + count,
                            "recipe resource plan: action=collect-catalogue-material"
                                    + ", material=" + catalogueMaterialName
                                    + ", need=" + count
                                    + ", met=false"
                                    + ", targets=" + describeTargets());
                    debugLogger.state("collect catalogue material:" + catalogueMaterialName + ":" + count,
                            "collect catalogue material: target=" + itemTarget
                                    + ", targets=" + describeTargets());
                    return TaskCatalogue.getItemTask(catalogueMaterialName, count);
                }
            }
        }
        for (Item item : itemCount.keySet()) {
            int count = itemCount.get(item);
            if (count > 0) {
                if (mod.getItemStorage().getItemCount(item) < count) {
                    setDebugState("Getting " + item.getTranslationKey());
                    logPlanEvent("item:" + item.getTranslationKey() + ":" + count,
                            "recipe resource plan: action=collect-concrete-item"
                                    + ", item=" + item.getTranslationKey()
                                    + ", have=" + mod.getItemStorage().getItemCount(item)
                                    + ", need=" + count
                                    + ", targets=" + describeTargets());
                    debugLogger.state("collect concrete item:" + item.getTranslationKey() + ":" + count,
                            "collect concrete item: item=" + item.getTranslationKey()
                                    + ", have=" + mod.getItemStorage().getItemCount(item)
                                    + ", need=" + count
                                    + ", targets=" + describeTargets());
                    return TaskCatalogue.getItemTask(item, count);
                }
            }
        }
        _finished = true;
        logPlanEvent("ready:" + describeTargets(),
                "recipe resource plan: action=resources-ready"
                        + ", targets=" + describeTargets());
        debugLogger.state("recipe resources ready:" + describeTargets(),
                "recipe resources ready: targets=" + describeTargets());

        return null;
    }


    @Override
    protected void onStop(Task interruptTask) {
        debugLogger.event("stop diagnostics: interruptedBy=" + describeTask(interruptTask)
                + ", finished=" + _finished
                + ", materialsReady=" + StorageHelper.hasRecipeMaterialsOrTarget(AltoClef.getInstance(), _targets)
                + ", targets=" + describeTargets());
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectRecipeCataloguedResourcesTask task) {
            return Arrays.equals(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect Recipe Resources: " + ArrayUtils.toString(_targets);
    }

    @Override
    public boolean isFinished() {
        if (_finished) {
            if (!StorageHelper.hasRecipeMaterialsOrTarget(AltoClef.getInstance(), this._targets)) {
                _finished = false;
                Debug.logMessage("Invalid collect recipe \"finished\" state, resetting.");
                debugLogger.event("finished state invalidated: targets=" + describeTargets());
            }
        }
        return _finished;
    }

    private String describeTargets() {
        return Arrays.toString(_targets);
    }

    private void logPlanEvent(String stateKey, String detail) {
        if (Objects.equals(lastPlanEventKey, stateKey)) {
            return;
        }
        lastPlanEventKey = stateKey;
        debugLogger.event(detail);
    }

    private String describeTask(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getSimpleName() + "{" + task + "}";
        } catch (RuntimeException ex) {
            return task.getClass().getSimpleName() + "{debugString failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "}";
        }
    }
}
