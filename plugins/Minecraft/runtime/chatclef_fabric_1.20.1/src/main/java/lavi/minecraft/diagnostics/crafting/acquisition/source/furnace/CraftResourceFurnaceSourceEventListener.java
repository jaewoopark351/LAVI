package lavi.minecraft.diagnostics.crafting.acquisition.source.furnace;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

//20260901_kpopmodder: Keep furnace source observation backend-neutral and side-effect free.
public interface CraftResourceFurnaceSourceEventListener {
    default void onChildSelection(
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
    }

    default void onMaterialProgress(
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
    }

    default void onOperationGate(
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
    }

    default void onContainerRoute(
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
    }
}
