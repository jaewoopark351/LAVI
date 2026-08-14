package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

//20260807_kpopmodder: Public furnace diagnostics facade; concrete observers stay split by responsibility.
public final class FurnaceContainerDiagnostics {
    public static final double PLACE_FORCE_DURATION_SECONDS = 1.0;
    public static final double JUST_PLACED_DURATION_SECONDS = 3.0;

    private FurnaceContainerDiagnostics() {
    }

    public static void markCacheUpdated(Task task, BlockPos sourceFurnacePosition) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        FurnaceTaskDiagnosticState.markCacheUpdated(task, sourceFurnacePosition);
    }

    public static void logRouteTransition(AltoClef mod,
                                          Task task,
                                          ItemTarget containerTarget,
                                          Block[] containerBlocks,
                                          String effectiveBranch,
                                          Task candidateChild,
                                          String candidateChildSemanticKey,
                                          BlockPos nearestPosition,
                                          String nearestSource,
                                          BlockPos overrideContainerPosition,
                                          BlockPos cachedContainerPositionBefore,
                                          BlockPos cachedContainerPositionAfter,
                                          BlockPos placeTaskPlaced,
                                          double costToWalk,
                                          double costToMakeNew,
                                          boolean placeForceElapsedBeforeReset,
                                          double placeForceDurationBeforeReset,
                                          boolean placeForceResetThisTick,
                                          boolean placeForceElapsedAfterReset,
                                          double placeForceDurationAfterReset,
                                          boolean justPlacedElapsed,
                                          double justPlacedTimerAgeSeconds,
                                          boolean hasContainerBlockItem,
                                          int containerBlockItemCount,
                                          String trigger) {
        FurnaceRouteDiagnostics.log(mod, task, containerTarget, containerBlocks, effectiveBranch, candidateChild,
                candidateChildSemanticKey, nearestPosition, nearestSource, overrideContainerPosition,
                cachedContainerPositionBefore, cachedContainerPositionAfter, placeTaskPlaced, costToWalk,
                costToMakeNew, placeForceElapsedBeforeReset, placeForceDurationBeforeReset,
                placeForceResetThisTick, placeForceElapsedAfterReset, placeForceDurationAfterReset,
                justPlacedElapsed, justPlacedTimerAgeSeconds, hasContainerBlockItem, containerBlockItemCount,
                trigger);
    }

    public static void logMakeCostSnapshot(AltoClef mod,
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
        FurnaceMakeCostDiagnostics.log(mod, task, costSource, costToMakeNew, furnaceCacheHasContents,
                cachedMaterialSlot, cachedFuelSlot, cachedOutputSlot, burningFuelCount, burnPercentage,
                cobblestoneCount, woodRequirementMetInventory, furnaceBlockItemCount);
    }

    public static void logOperationGate(AltoClef mod,
                                        Task task,
                                        String currentGate,
                                        int inventoryMaterialCount,
                                        int materialsNeeded,
                                        boolean materialGateSatisfied,
                                        double inventoryFuelCount,
                                        double fuelNeeded,
                                        boolean fuelGateSatisfied,
                                        Object materialsAccessible,
                                        boolean containerFlowEligible,
                                        int inventoryOutputCount) {
        FurnaceOperationGateDiagnostics.log(mod, task, currentGate, inventoryMaterialCount, materialsNeeded,
                materialGateSatisfied, inventoryFuelCount, fuelNeeded, fuelGateSatisfied, materialsAccessible,
                containerFlowEligible, inventoryOutputCount);
    }

    public static void logMaterialProgressSnapshot(AltoClef mod,
                                                   Task task,
                                                   ItemTarget materialTarget,
                                                   ItemTarget outputTarget,
                                                   String currentGate,
                                                   int inventoryMaterialCount,
                                                   int inventoryOutputCount,
                                                   int materialsNeeded,
                                                   double inventoryFuelCount,
                                                   double fuelNeeded,
                                                   boolean materialGateSatisfied,
                                                   boolean fuelGateSatisfied,
                                                   ItemStack cachedMaterialSlot,
                                                   ItemStack cachedFuelSlot,
                                                   ItemStack cachedOutputSlot,
                                                   double burningFuelCount,
                                                   double burnPercentage) {
        FurnaceMaterialProgressDiagnostics.log(mod, task, materialTarget, outputTarget, currentGate,
                inventoryMaterialCount, inventoryOutputCount, materialsNeeded, inventoryFuelCount, fuelNeeded,
                materialGateSatisfied, fuelGateSatisfied, cachedMaterialSlot, cachedFuelSlot, cachedOutputSlot,
                burningFuelCount, burnPercentage);
    }

    public static void logChildSelection(AltoClef mod,
                                         Task task,
                                         Task candidateChild,
                                         String currentGate,
                                         String candidateChildSemanticKey,
                                         int inventoryMaterialCount,
                                         int inventoryOutputCount,
                                         int materialsNeeded,
                                         double inventoryFuelCount,
                                         double fuelNeeded,
                                         boolean materialGateSatisfied,
                                         boolean fuelGateSatisfied) {
        FurnaceChildSelectionDiagnostics.log(mod, task, candidateChild, currentGate, candidateChildSemanticKey,
                inventoryMaterialCount, inventoryOutputCount, materialsNeeded, inventoryFuelCount, fuelNeeded,
                materialGateSatisfied, fuelGateSatisfied);
    }
}
