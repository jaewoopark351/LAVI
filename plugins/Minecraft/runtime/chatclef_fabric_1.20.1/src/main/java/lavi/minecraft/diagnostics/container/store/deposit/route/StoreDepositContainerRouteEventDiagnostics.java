package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

//20260829_kpopmodder: Keep container route event observation in one focused collaborator.
public final class StoreDepositContainerRouteEventDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositContainerRouteEventDiagnostics(StoreDepositBindingRegistry bindings,
                                                      StoreDepositEmissionGate emissionGate) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
    }

    public void observeContainerRouteEvent(String eventName,
                                           String reason,
                                           Task task,
                                           Object[] branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordCraftRouteEvent(eventName, reason);
            if (!"CONTAINER_TASK_TARGET_DECISION".equals(eventName)) {
                return;
            }
            String operationId = StoreDepositEventFields.operationId(state);
            String key = state.context().isDepositAllOperation()
                    ? operationId
                            + "|" + className(task)
                            + "|" + eventName
                            + "|" + reason
                    : operationId
                            + "|" + StoreDepositEventFields.identity(task)
                            + "|" + eventName
                            + "|" + reason;
            if (!emissionGate.shouldEmitDetail(operationId, "STORE_CRAFT_ROUTE_EVALUATION_ENTERED", key)) {
                return;
            }
            StoreDepositBoundedEventLogger.log("STORE_CRAFT_ROUTE_EVALUATION_ENTERED",
                    "store_craft_route_evaluation_entered",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.craftRouteEventFields(state, task, eventName, reason, branchFields)
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }
}
