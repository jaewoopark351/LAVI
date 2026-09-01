package lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;

//20260902_kpopmodder: Expose the existing container owner stop callback without inferring command ownership.
public interface CraftResourceContainerLifecycleSourceEventListener {
    default boolean isOwnerTracked(Task owner) {
        return false;
    }

    void onOwnerStop(
            Task owner,
            Task interruptTask,
            CraftResourceTargetObservationKind terminationKind,
            boolean sourceEmissionCompleted
    );
}
