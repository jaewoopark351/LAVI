package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.ItemStack;

//20260807_kpopmodder: Observe make-new furnace cost source transitions without changing cost calculation.
final class FurnaceMakeCostDiagnostics {
    private FurnaceMakeCostDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    String costSource,
                    double costToMakeNew,
                    boolean furnaceCacheHasContents,
                    ItemStack cachedMaterialSlot,
                    ItemStack cachedFuelSlot,
                    ItemStack cachedOutputSlot,
                    double burningFuelCount,
                    double burnPercentage,
                    int cobblestoneCount,
                    boolean woodRequirementMetInventory,
                    int furnaceBlockItemCount) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        FurnaceTaskDiagnosticState.CostObservation cost = FurnaceTaskDiagnosticState.observeCost(task, costSource);
        if (!cost.sourceChanged) {
            return;
        }
        FurnaceTaskDiagnosticState.CacheObservation cache = FurnaceTaskDiagnosticState.cacheObservation(task);
        String fingerprint = FurnaceDiagnosticEmitter.joinFingerprint(
                "FURNACE_MAKE_COST_SNAPSHOT",
                cost.previousCostSource,
                cost.currentCostSource,
                Boolean.toString(furnaceCacheHasContents),
                cobblestoneBand(cobblestoneCount),
                Boolean.toString(woodRequirementMetInventory)
        );
        FurnaceDiagnosticEmitter.emit("FURNACE_MAKE_COST_SNAPSHOT", "furnace_make_cost_snapshot", task,
                "furnace_make_cost|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "furnace_make_cost_observer",
                        "trigger", "cost_source_transition",
                        "previousCostSource", cost.previousCostSource,
                        "costSource", cost.currentCostSource,
                        "costSourceChanged", cost.sourceChanged,
                        "costSourceTransitionCount", cost.sourceTransitionCount,
                        "costToMakeNew", costToMakeNew,
                        "furnaceCacheHasContents", furnaceCacheHasContents,
                        "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(cachedMaterialSlot),
                        "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(cachedFuelSlot),
                        "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(cachedOutputSlot),
                        "burningFuelCount", burningFuelCount,
                        "burnPercentage", burnPercentage,
                        "cacheUpdatedThisTick", cache.cacheUpdatedThisTick,
                        "cacheLastUpdatedTick", cache.cacheLastUpdatedTick,
                        "cacheAgeTicks", cache.cacheAgeTicks,
                        "cacheSourceFurnacePosition", cache.cacheSourceFurnacePosition,
                        "cobblestoneCount", cobblestoneCount,
                        "woodRequirementMetInventory", woodRequirementMetInventory,
                        "furnaceBlockItemCount", furnaceBlockItemCount
                });
    }

    private static String cobblestoneBand(int cobblestoneCount) {
        if (cobblestoneCount <= 0) {
            return "ZERO";
        }
        if (cobblestoneCount <= 8) {
            return "ONE_TO_EIGHT";
        }
        return "GT_EIGHT";
    }
}
