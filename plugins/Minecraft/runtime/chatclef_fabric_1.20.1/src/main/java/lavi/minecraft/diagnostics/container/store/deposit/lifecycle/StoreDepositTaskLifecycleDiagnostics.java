package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositTerminalSummaryEmitter;

//20260829_kpopmodder: Keep Task lifecycle observation and its terminal handoff in one focused collaborator.
public final class StoreDepositTaskLifecycleDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositTerminalSummaryEmitter terminalSummaries;

    public StoreDepositTaskLifecycleDiagnostics(StoreDepositBindingRegistry bindings,
                                                StoreDepositEmissionGate emissionGate,
                                                StoreDepositTerminalSummaryEmitter terminalSummaries) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.terminalSummaries = terminalSummaries;
    }

    public void logTaskLifecycleBoundary(Task task,
                                         Task interruptTask,
                                         String action,
                                         String phase,
                                         boolean activeBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            String role = bindings.roleFor(task);
            boolean rootStop = "ROOT_STORE".equals(role) && "STOP".equals(action);
            state.recordLifecycle(
                    task,
                    action,
                    phase,
                    rootStop,
                    ChatClefDiagnostics.currentClientTickId()
            );
            boolean terminalPending = rootStop && "BEGIN".equals(phase);
            boolean operationFinalized = rootStop && "END".equals(phase);
            String operationId = StoreDepositEventFields.operationId(state);
            String key = state.context().isDepositAllOperation()
                    ? operationId
                            + "|" + role
                            + "|" + className(task)
                            + "|" + action
                            + "|" + phase
                    : operationId
                            + "|" + StoreDepositEventFields.identity(task)
                            + "|" + action
                            + "|" + phase;
            if (emissionGate.shouldEmitDetail(operationId, "STORE_TASK_LIFECYCLE_BOUNDARY", key)) {
                StoreDepositBoundedEventLogger.log("STORE_TASK_LIFECYCLE_BOUNDARY",
                        "store_task_lifecycle_boundary",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.lifecycleFields(state, task, interruptTask, action, phase, role, activeBefore, terminalPending, operationFinalized)
                        ));
            }
            if (operationFinalized) {
                terminalSummaries.emitAndPurge(
                        task,
                        state,
                        "ROOT_STOP_END",
                        state.explicitStopCorrelated() ? "EXPLICIT_STOP_CORRELATED" : "UNKNOWN_STOP"
                );
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void logNaturalFinish(Task task) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null || !state.context().isRoot(task)) {
                return;
            }
            state.recordNaturalFinish();
            StoreDepositBoundedEventLogger.log("STORE_TASK_LIFECYCLE_BOUNDARY",
                    "store_task_natural_finish_observed",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.lifecycleFields(state, task, null, "NATURAL_FINISH", "OBSERVED", "ROOT_STORE", true, false, true)
                    ));
            terminalSummaries.emitAndPurge(task, state, "NATURAL_FINISH", "NATURAL_FINISH_OBSERVED");
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }
}
