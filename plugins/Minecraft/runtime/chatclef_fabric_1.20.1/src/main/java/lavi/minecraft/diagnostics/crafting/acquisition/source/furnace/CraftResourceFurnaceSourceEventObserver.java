package lavi.minecraft.diagnostics.crafting.acquisition.source.furnace;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

//20260901_kpopmodder: Isolate optional furnace projections from authoritative diagnostics.
public final class CraftResourceFurnaceSourceEventObserver {
    private static final CraftResourceFurnaceSourceEventListener NO_OP =
            new CraftResourceFurnaceSourceEventListener() {
            };
    private static volatile CraftResourceFurnaceSourceEventListener listener = NO_OP;

    private CraftResourceFurnaceSourceEventObserver() {
    }

    public static void install(CraftResourceFurnaceSourceEventListener installed) {
        listener = installed == null ? NO_OP : installed;
    }

    public static void observeChildSelection(
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
        try {
            listener.onChildSelection(
                    task,
                    candidateChild,
                    currentGate,
                    candidateChildSemanticKey,
                    inventoryMaterialCount,
                    inventoryOutputCount,
                    materialsNeeded,
                    inventoryFuelCount,
                    fuelNeeded,
                    materialGateSatisfied,
                    fuelGateSatisfied
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }

    public static void observeMaterialProgress(
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
            double burningFuelCount,
            double burnPercentage,
            boolean sourceEmissionCompleted) {
        try {
            listener.onMaterialProgress(
                    task,
                    materialTarget,
                    outputTarget,
                    currentGate,
                    inventoryMaterialCount,
                    inventoryOutputCount,
                    materialsNeeded,
                    inventoryFuelCount,
                    fuelNeeded,
                    materialGateSatisfied,
                    fuelGateSatisfied,
                    burningFuelCount,
                    burnPercentage,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }

    public static void observeOperationGate(
            Task task,
            String previousGate,
            String currentGate,
            int inventoryMaterialCount,
            int materialsNeeded,
            boolean materialGateSatisfied,
            double inventoryFuelCount,
            double fuelNeeded,
            boolean fuelGateSatisfied,
            Object materialsAccessible,
            boolean containerFlowEligible,
            int inventoryOutputCount,
            boolean sourceEmissionCompleted) {
        try {
            listener.onOperationGate(
                    task,
                    previousGate,
                    currentGate,
                    inventoryMaterialCount,
                    materialsNeeded,
                    materialGateSatisfied,
                    inventoryFuelCount,
                    fuelNeeded,
                    fuelGateSatisfied,
                    materialsAccessible,
                    containerFlowEligible,
                    inventoryOutputCount,
                    sourceEmissionCompleted
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }

    public static void observeContainerRoute(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String previousEffectiveBranch,
            String effectiveBranch,
            Task candidateChild,
            String candidateChildSemanticKey,
            BlockPos nearestPosition,
            String nearestSource,
            BlockPos cachedContainerPositionAfter,
            BlockPos placeTaskPlaced,
            boolean hasContainerBlockItem,
            int containerBlockItemCount,
            String trigger) {
        try {
            listener.onContainerRoute(
                    task,
                    containerTarget,
                    containerBlocks,
                    previousEffectiveBranch,
                    effectiveBranch,
                    candidateChild,
                    candidateChildSemanticKey,
                    nearestPosition,
                    nearestSource,
                    cachedContainerPositionAfter,
                    placeTaskPlaced,
                    hasContainerBlockItem,
                    containerBlockItemCount,
                    trigger
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }
}
