package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;

//20260829_kpopmodder: Keep tracker-to-operation binding in one focused lifecycle collaborator.
public final class StoreDepositTrackerBindingDiagnostics {
    private final StoreDepositBindingRegistry bindings;

    public StoreDepositTrackerBindingDiagnostics(StoreDepositBindingRegistry bindings) {
        this.bindings = bindings;
    }

    public void bindRootTracker(Task owner, ContainerStoredTracker tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            bindings.bindTracker(owner, tracker, "ROOT_ANY_CONTAINER", null);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void bindTargetTracker(Task owner,
                                  ContainerStoredTracker tracker,
                                  BlockPos targetContainer) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            bindings.bindTracker(owner, tracker, "TARGET_CONTAINER", targetContainer);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void trackerSubscriptionStarted(ContainerStoredTracker tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(tracker);
            if (isAutomatic(state)) {
                bindings.markTrackerSubscriptionStarted(tracker);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void trackerSubscriptionStopped(ContainerStoredTracker tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(tracker);
            if (isAutomatic(state)) {
                bindings.markTrackerSubscriptionStopped(tracker);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static boolean isAutomatic(StoreDepositOperationState state) {
        return state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && state.automaticContext().available();
    }
}
