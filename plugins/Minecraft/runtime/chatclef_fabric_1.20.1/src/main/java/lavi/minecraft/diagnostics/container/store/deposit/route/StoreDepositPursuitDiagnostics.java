package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.util.math.BlockPos;

//20260829_kpopmodder: Keep pursuit-decision observation in one focused route collaborator.
public final class StoreDepositPursuitDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositPursuitDiagnostics(StoreDepositBindingRegistry bindings,
                                          StoreDepositEmissionGate emissionGate) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
    }

    public void logPursuitDecision(Task task,
                                   Object currentPursuit,
                                   Object candidate,
                                   String returnedAction) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordPursuitDecision(returnedAction);
            if (state.context().isDepositAllOperation()) {
                state.routeState().recordPursuit(candidate, returnedAction);
            }
            String operationId = StoreDepositEventFields.operationId(state);
            String key = state.context().isDepositAllOperation()
                    ? operationId
                            + "|" + returnedAction
                            + "|" + diagnosticPosition(candidate)
                    : operationId
                            + "|" + StoreDepositEventFields.identity(task)
                            + "|" + returnedAction
                            + "|" + String.valueOf(candidate);
            if (!emissionGate.shouldEmitDetail(operationId, "STORE_CONTAINER_PURSUIT_DECISION", key)) {
                return;
            }
            Object[] eventFields = StoreDepositEventFields.pursuitDecisionFields(
                    state,
                    task,
                    currentPursuit,
                    candidate,
                    returnedAction
            );
            if (state.context().isDepositAllOperation()) {
                eventFields = StoreDepositEventFields.merge(
                        eventFields,
                        StoreContainerCandidateEventFields.routeCorrelationFields(state)
                );
            }
            StoreDepositBoundedEventLogger.log("STORE_CONTAINER_PURSUIT_DECISION",
                    "store_container_pursuit_decision",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(eventFields));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String diagnosticPosition(Object candidate) {
        return candidate instanceof BlockPos position ? position.toShortString() : className(candidate);
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }
}
