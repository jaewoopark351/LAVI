package lavi.minecraft.diagnostics.container.store.deposit.event.lifecycle;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;

//20260902_kpopmodder: Own lifecycle-family diagnostic payload assembly behind the stable public facade.
public final class StoreDepositLifecycleEventFields {
    private StoreDepositLifecycleEventFields() {
    }

    public static Object[] rootActivationFields(StoreDepositOperationState state, ItemTarget[] targets) {
        Object[] fields = StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "storeActivationEpoch", state == null ? "unavailable" : state.activationCount(),
                        "activationKind", state == null || state.activationCount() <= 1
                                ? "INITIAL"
                                : "RESUME_AFTER_INTERRUPT",
                        "requestedTargetItems", ChatClefDiagnostics.itemTargets(targets)
                }
        );
        return fields;
    }

    public static Object[] lifecycleFields(StoreDepositOperationState state,
                                           Task task,
                                           Task interruptTask,
                                           String action,
                                           String phase,
                                           String lifecycleTaskRole,
                                           boolean activeBefore,
                                           boolean terminalPending,
                                           boolean operationFinalized) {
        Object[] fields = StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_lifecycle",
                        "owner", "store_deposit_lifecycle_observer",
                        "mode", "BOUNDARY",
                        "trigger", action + "_" + phase,
                        "dedupe_key", "store_task_lifecycle|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + StoreDepositOperationCorrelationFields.identity(task) + "|" + action + "|" + phase,
                        "max_emission", "state_change_only,session_cap=5000",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",taskIdentity=" + StoreDepositOperationCorrelationFields.identity(task),
                        "payload", "flat_fields",
                        "terminal", operationFinalized,
                        "behavior_effect", "none",
                        "lifecycleAction", action,
                        "lifecyclePhase", phase,
                        "taskActiveBefore", activeBefore,
                        "lifecycleTaskRole", lifecycleTaskRole,
                        "interruptTaskInstanceId", StoreDepositOperationCorrelationFields.identity(interruptTask),
                        "interruptTaskClass", interruptTask == null ? "none" : interruptTask.getClass().getName(),
                        "onStopCallbackExpected", !"END".equals(phase),
                        "terminalPending", terminalPending,
                        "operationFinalized", operationFinalized,
                        "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
                }
        );
        if (state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && "STOP".equals(action)
                && state.routeState().isCurrentRouteChild(task)) {
            return StoreDepositOrderedFieldSupport.merge(
                    fields,
                    StoreDepositOperationCorrelationFields.activeRouteIdentityFields(state)
            );
        }
        return fields;
    }

    public static Object[] childReconciliationFields(StoreDepositOperationState state,
                                                     Task parent,
                                                     Task activeChildBefore,
                                                     Task candidateChild,
                                                     Task activeChildAfter,
                                                     boolean subTasksEqual,
                                                     boolean canInterruptEvaluated,
                                                     boolean canInterrupt,
                                                     boolean replacementApplied,
                                                     boolean previousChildStopCalled,
                                                     String lifecycleTaskRole,
                                                     String reconciliationRole) {
        String outcome = replacementApplied
                && candidateChild == null
                ? "ACTIVE_CHILD_CLEARED"
                : replacementApplied
                ? "CHILD_REPLACED"
                : subTasksEqual
                ? "ACTIVE_CHILD_RETAINED"
                : candidateChild == null ? "NULL_CHILD_RESULT" : "CANDIDATE_NOT_INSTALLED";
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_child_reconciliation",
                        "owner", "store_deposit_child_reconciliation_observer",
                        "mode", "BOUNDARY",
                        "trigger", "task_tick_child_reconciliation",
                        "dedupe_key", "store_child_reconciliation|" + StoreDepositOperationCorrelationFields.operationId(state) + "|" + StoreDepositOperationCorrelationFields.identity(parent) + "|" + outcome + "|" + className(candidateChild),
                        "max_emission", "state_change_only,per_operation=256",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state) + ",parentTaskIdentity=" + StoreDepositOperationCorrelationFields.identity(parent),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "lifecycleTaskRole", lifecycleTaskRole,
                        "reconciliationRole", reconciliationRole,
                        "reconciliationOutcome", outcome,
                        "parentTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(parent),
                        "activeChildBefore", ChatClefDiagnostics.taskSummaryForDiagnosticLog(activeChildBefore),
                        "candidateChild", ChatClefDiagnostics.taskSummaryForDiagnosticLog(candidateChild),
                        "activeChildAfter", ChatClefDiagnostics.taskSummaryForDiagnosticLog(activeChildAfter),
                        "subTasksEqual", subTasksEqual,
                        "canInterruptEvaluated", canInterruptEvaluated,
                        "canInterruptPreviousChild", canInterrupt,
                        "replacementApplied", replacementApplied,
                        "previousChildStopCalled", previousChildStopCalled
                }
        );
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }
}
