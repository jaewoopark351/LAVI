package adris.altoclef.tasks.resources.wood;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CraftWithMatchingPlanksTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

public class CollectWoodenButtonTask extends CraftWithMatchingPlanksTask {

    public CollectWoodenButtonTask(Item[] targets, ItemTarget planks, int count) {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // Incident wooden-button-stack-overflow-2026-09-05-235653-kst; see
        // plugins/Minecraft/docs/chatclef-wooden-button-stack-overflow-pre-change-report-2026-09-06.md.
        super(targets, woodItems -> woodItems.button, createRecipe(planks), new boolean[]{true, false, false, false}, count);
    }

    public CollectWoodenButtonTask(Item target, String plankCatalogueName, int count) {
        this(new Item[]{target}, new ItemTarget(plankCatalogueName, 1), count);
    }

    public CollectWoodenButtonTask(int count) {
        this(ItemHelper.WOOD_BUTTON, TaskCatalogue.getItemTarget("planks", 1), count);
    }


    private static CraftingRecipe createRecipe(ItemTarget planks) {
        ItemTarget p = planks;
        return CraftingRecipe.newShapedRecipe(new ItemTarget[]{p, null, null, null}, 1);
    }
}
