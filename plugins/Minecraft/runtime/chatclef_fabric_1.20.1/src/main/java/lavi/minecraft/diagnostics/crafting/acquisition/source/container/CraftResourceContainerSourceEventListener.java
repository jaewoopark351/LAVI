package lavi.minecraft.diagnostics.crafting.acquisition.source.container;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;

//20260901_kpopmodder: Expose only the already-emitted generic container decision.
public interface CraftResourceContainerSourceEventListener {
    void onTargetDecision(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields
    );

    default void onChildSelection(
            Task task,
            Task candidateChild,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String reason,
            String stateKey,
            Object[] branchFields,
            boolean sourceEmissionCompleted) {
    }
}
