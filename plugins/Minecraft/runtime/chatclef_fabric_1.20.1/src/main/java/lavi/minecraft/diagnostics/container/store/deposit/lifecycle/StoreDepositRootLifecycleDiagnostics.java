package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

//20260829_kpopmodder: Keep root activation, stop context, and explicit-stop correlation together.
public final class StoreDepositRootLifecycleDiagnostics {
    private final StoreDepositBindingRegistry bindings;

    public StoreDepositRootLifecycleDiagnostics(StoreDepositBindingRegistry bindings) {
        this.bindings = bindings;
    }

    public Object[] onStoreRootStart(Task task, boolean getIfNotPresent, ItemTarget[] toStore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            StoreDepositOperationState state = bindings.activateRoot(task, "STORE_IN_ANY_CONTAINER_TASK");
            state.recordRequestedTargets(toStore);
            return StoreDepositEventFields.rootActivationFields(state, toStore);
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public Object[] onStoreRootStopCallback(Task task, Task interruptTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            return StoreDepositEventFields.merge(
                    StoreDepositEventFields.operationFields(bindings.stateFor(task)),
                    new Object[]{
                            "storeStopCallbackOnly", true,
                            "storeStopCallbackIsTerminalAuthority", false,
                            "storeInterruptTaskInstanceId", StoreDepositEventFields.identity(interruptTask),
                            "storeInterruptTaskClass", interruptTask == null ? "none" : interruptTask.getClass().getName()
                    });
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public void markExplicitCancelCandidate(Task rootTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(rootTask);
            if (state != null && state.context().isRoot(rootTask)) {
                state.recordExplicitStopCorrelation();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
