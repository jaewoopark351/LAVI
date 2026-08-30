package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteCandidateReconciliation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositMovementDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferDiagnostics;

//20260829_kpopmodder: Isolate child handoff observation while preserving shared operation state and emission ordering.
public final class StoreDepositChildReconciliationDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositMovementDiagnostics movementDiagnostics;
    private final StoreDepositTransferDiagnostics transferDiagnostics;

    public StoreDepositChildReconciliationDiagnostics(StoreDepositBindingRegistry bindings,
                                                       StoreDepositEmissionGate emissionGate) {
        this(bindings, emissionGate, null, null);
    }

    public StoreDepositChildReconciliationDiagnostics(StoreDepositBindingRegistry bindings,
                                                       StoreDepositEmissionGate emissionGate,
                                                       StoreDepositMovementDiagnostics movementDiagnostics) {
        this(bindings, emissionGate, movementDiagnostics, null);
    }

    public StoreDepositChildReconciliationDiagnostics(StoreDepositBindingRegistry bindings,
                                                       StoreDepositEmissionGate emissionGate,
                                                       StoreDepositMovementDiagnostics movementDiagnostics,
                                                       StoreDepositTransferDiagnostics transferDiagnostics) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.movementDiagnostics = movementDiagnostics;
        this.transferDiagnostics = transferDiagnostics;
    }

    public void logChildReconciliation(Task parent,
                                       Task activeChildBefore,
                                       Task candidateChild,
                                       boolean subTasksEqual,
                                       boolean canInterruptEvaluated,
                                       boolean canInterrupt,
                                       boolean replacementApplied,
                                       boolean previousChildStopCalled,
                                       Task activeChildAfter) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(parent);
            if (state == null) {
                return;
            }
            if (activeChildAfter != null) {
                bindings.bindChild(parent, activeChildAfter);
            }
            if (candidateChild != null && replacementApplied) {
                bindings.bindChild(parent, candidateChild);
            }
            String outcome = replacementApplied
                    && candidateChild == null
                    ? "ACTIVE_CHILD_CLEARED"
                    : replacementApplied
                    ? "CHILD_REPLACED"
                    : subTasksEqual ? "ACTIVE_CHILD_RETAINED" : candidateChild == null ? "NULL_CHILD_RESULT" : "CANDIDATE_NOT_INSTALLED";
            String lifecycleRole = bindings.roleFor(parent);
            String reconciliationRole = reconciliationRole(parent, lifecycleRole);
            state.recordChildReconciliation(reconciliationRole + ":" + outcome, activeChildAfter);
            boolean previousRouteChildStopObserved = previousChildStopCalled
                    && state.wasStopCompletedFor(
                            activeChildBefore,
                            ChatClefDiagnostics.currentClientTickId()
                    );
            RouteCandidateReconciliation routeReconciliation = null;
            if (movementDiagnostics != null) {
                routeReconciliation = movementDiagnostics.reconcileRouteCandidate(
                        state,
                        parent,
                        activeChildBefore,
                        candidateChild,
                        subTasksEqual,
                        replacementApplied,
                        activeChildAfter
                );
            }
            boolean resourceAcquisitionInterruptedByBranchChange = false;
            if (state.context().isDepositAllOperation()) {
                resourceAcquisitionInterruptedByBranchChange = state.routeState().recordChildReconciliation(
                        reconciliationRole,
                        activeChildBefore,
                        activeChildAfter,
                        replacementApplied,
                        previousRouteChildStopObserved
                );
            }
            if (movementDiagnostics != null) {
                movementDiagnostics.expectActiveRouteChildTerminal(
                        state,
                        reconciliationRole,
                        replacementApplied,
                        activeChildAfter
                );
                movementDiagnostics.recordRouteReconciliationTerminal(
                        state,
                        parent,
                        candidateChild,
                        routeReconciliation
                );
            }
            if (transferDiagnostics != null) {
                transferDiagnostics.reconcileTransferCandidate(
                        parent,
                        activeChildBefore,
                        candidateChild,
                        subTasksEqual,
                        replacementApplied,
                        activeChildAfter
                );
            }
            String key = state.context().isDepositAllOperation()
                    ? StoreDepositEventFields.operationId(state)
                            + "|" + reconciliationRole
                            + "|" + outcome
                            + "|" + state.routeState().currentParentDecision().previousBranch()
                            + "|" + state.routeState().currentBranch()
                            + "|" + state.routeState().currentRangeTransition().rawRangeCrossing()
                            + "|" + state.routeState().currentRangeTransition().currentTryRangeCrossing()
                            + "|" + resourceAcquisitionInterruptedByBranchChange
                            + "|" + (routeReconciliation == null || !routeReconciliation.available()
                                    ? "generation-unavailable"
                                    : routeReconciliation.stagedCandidateGeneration())
                            + "|" + StoreDepositEventFields.identity(activeChildBefore)
                            + "|" + StoreDepositEventFields.identity(candidateChild)
                            + "|" + StoreDepositEventFields.identity(activeChildAfter)
                    : StoreDepositEventFields.operationId(state)
                            + "|" + StoreDepositEventFields.identity(parent)
                            + "|" + outcome
                            + "|" + StoreDepositEventFields.identity(activeChildAfter);
            if (!emissionGate.shouldEmitDetail(
                    StoreDepositEventFields.operationId(state),
                    "STORE_TASK_CHILD_RECONCILIATION",
                    key
            )) {
                return;
            }
            Object[] eventFields = StoreDepositEventFields.childReconciliationFields(
                    state,
                    parent,
                    activeChildBefore,
                    candidateChild,
                    activeChildAfter,
                    subTasksEqual,
                    canInterruptEvaluated,
                    canInterrupt,
                    replacementApplied,
                    previousChildStopCalled,
                    lifecycleRole,
                    reconciliationRole
            );
            if (state.context().isDepositAllOperation()) {
                eventFields = StoreDepositEventFields.merge(
                        StoreDepositEventFields.merge(
                                eventFields,
                                StoreContainerCandidateEventFields.childHandoffFields(
                                        state,
                                        activeChildBefore,
                                        candidateChild,
                                        activeChildAfter
                                )
                        ),
                        StoreContainerRangeEventFields.childReconciliationFields(
                                state.routeState().currentRangeTransition(),
                                previousRouteChildStopObserved,
                                resourceAcquisitionInterruptedByBranchChange
                        )
                );
                if (movementDiagnostics != null) {
                    eventFields = StoreDepositEventFields.merge(
                            eventFields,
                            movementDiagnostics.reconciliationFields(
                                    state,
                                    routeReconciliation,
                                    activeChildBefore,
                                    candidateChild,
                                    activeChildAfter
                            )
                    );
                }
            }
            StoreDepositBoundedEventLogger.log(
                    "STORE_TASK_CHILD_RECONCILIATION",
                    "store_task_child_reconciliation",
                    parent,
                    ChatClefDiagnostics.withCommandContextFields(eventFields)
            );
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }

    private static String reconciliationRole(Task parent, String lifecycleRole) {
        if ("ROOT_STORE".equals(lifecycleRole)) {
            return "ROOT_ROUTE";
        }
        String className = parent == null ? "" : parent.getClass().getName();
        if (className.endsWith(".DoToClosestBlockTask")) {
            return "TARGET_ACTION";
        }
        if (className.contains(".tasks.container.")) {
            return "CRAFT_OR_TRANSFER_ROUTE";
        }
        return "DESCENDANT";
    }
}
