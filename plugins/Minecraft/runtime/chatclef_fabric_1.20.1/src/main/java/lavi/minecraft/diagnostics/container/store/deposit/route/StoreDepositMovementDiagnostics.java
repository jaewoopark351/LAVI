package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteCandidateReconciliation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteCandidateStage;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteState.RouteMovementObservation;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Separate selected-candidate invalidation from generic Task child reconciliation.
public final class StoreDepositMovementDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;

    public StoreDepositMovementDiagnostics(StoreDepositBindingRegistry bindings,
                                           StoreDepositEmissionGate emissionGate,
                                           StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.automaticTerminals = automaticTerminals;
    }

    public void observeMovementResult(Task rootTask,
                                      BlockPos selectedTargetBefore,
                                      String invalidationReason,
                                      boolean progressCheckEvaluated,
                                      boolean progressCheckOk,
                                      boolean candidateResetPerformed) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositOperationState state = bindings.stateFor(rootTask);
        if (!isAutomatic(state)) {
            return;
        }
        RouteMovementObservation observation = state.routeState().recordMovementObservation(
                selectedTargetBefore,
                invalidationReason,
                progressCheckEvaluated,
                progressCheckOk,
                candidateResetPerformed
        );
        String operationId = state.context().operationId();
        String invocationId = observation.progressCheckInvocationId() < 0
                ? "UNAVAILABLE"
                : operationId + "-progress-" + observation.progressCheckInvocationId();
        String generationBefore = candidateId(
                operationId,
                observation.selectedCandidateGenerationBefore()
        );
        String generationAfter = candidateId(
                operationId,
                observation.selectedCandidateGenerationAfter()
        );
        String semanticKey = generationBefore
                + "|" + observation.progressCheckEvaluated()
                + "|" + observation.progressCheckOk()
                + "|" + observation.invalidationReason()
                + "|" + generationBefore
                + "|" + generationAfter;
        if (emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_MOVEMENT_RESULT",
                semanticKey
        )) {
            StoreDepositBoundedEventLogger.log(
                    "STORE_DEPOSIT_MOVEMENT_RESULT",
                    "store_deposit_movement_result",
                    rootTask,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.merge(
                                    StoreDepositEventFields.operationFields(state),
                                    new Object[]{
                                            "progressCheckInvocationId", invocationId,
                                            "progressCheckEvaluated", observation.progressCheckEvaluated(),
                                            "progressCheckOk", observation.progressCheckEvaluated()
                                                    ? observation.progressCheckOk()
                                                    : "UNAVAILABLE",
                                            "progressMode", "UNAVAILABLE",
                                            "progressBaseline", "UNAVAILABLE",
                                            "progressElapsed", "UNAVAILABLE",
                                            "progressResetProvenance", observation.candidateResetPerformed()
                                                    ? "SELECTED_CANDIDATE_INVALIDATION"
                                                    : "UNAVAILABLE",
                                            "selectedTargetBefore", ChatClefDiagnostics.blockPos(observation.selectedTargetBefore()),
                                            "selectedTargetAfter", ChatClefDiagnostics.blockPos(observation.selectedTargetAfter()),
                                            "selectedCandidateGenerationBefore", generationBefore,
                                            "selectedCandidateGenerationAfter", generationAfter,
                                            "selectedCandidateGenerationId", generationBefore,
                                            "candidateInvalidationReason", observation.invalidationReason(),
                                            "activeRouteChildBefore", state.routeState().currentRouteChildIdentity(),
                                            "progressResultObserved", observation.progressCheckEvaluated(),
                                            "observationComplete", false,
                                            "missingBoundaries", observation.progressCheckEvaluated()
                                                    ? "PROGRESS_MODE,BASELINE,ELAPSED"
                                                    : "PROGRESS_CHECK_NOT_EVALUATED,MODE,BASELINE,ELAPSED",
                                            "behavior_effect", "none"
                                    }
                            )
                    )
            );
        }
        if (candidateResetPerformed && !"NONE".equals(observation.invalidationReason())) {
            automaticTerminals.expectScopeIdentity(
                    state.automaticContext(),
                    "CANDIDATE_INVALIDATION",
                    generationBefore
            );
            automaticTerminals.recordScopeIdentity(
                    state.automaticContext(),
                    "CANDIDATE_INVALIDATION",
                    generationBefore,
                    rootTask,
                    StoreDepositOperationContext.identity(rootTask),
                    StoreDepositOperationContext.identity(rootTask),
                    observation.invalidationReason() + "|" + generationBefore,
                    StoreDepositEventFields.merge(
                            StoreDepositEventFields.operationFields(state),
                            new Object[]{"selectedCandidateGenerationId", generationBefore}
                    )
            );
        }
    }

    public void stageRouteCandidate(Task rootTask,
                                    Task candidateTask,
                                    BlockPos selectedTarget,
                                    int behaviorGenerationId,
                                    boolean selectedNewTarget) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositOperationState state = bindings.stateFor(rootTask);
        if (!isAutomatic(state) || candidateTask == null) {
            return;
        }
        RouteCandidateStage stage = state.routeState().stageRouteCandidate(
                candidateTask,
                selectedTarget,
                behaviorGenerationId,
                selectedNewTarget
        );
        automaticTerminals.expectScopeIdentity(
                state.automaticContext(),
                "ROUTE_RECONCILIATION",
                candidateId(state.context().operationId(), stage.selectedCandidateGeneration())
        );
    }

    public RouteCandidateReconciliation reconcileRouteCandidate(
            StoreDepositOperationState state,
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean subTasksEqual,
            boolean replacementApplied,
            Task activeChildAfter) {
        if (!isAutomatic(state) || !state.context().isRoot(parent)) {
            return RouteCandidateReconciliation.unavailable(
                    state == null ? -1L : state.routeState().activeRouteCandidateGeneration()
            );
        }
        String operationId = state.context().operationId();
        long previousRouteChildLifecycleSequence =
                state.routeState().activeStoreAttemptRouteChildLifecycleSequence();
        Object[] previousRouteCorrelation = StoreDepositEventFields.activeRouteIdentityFields(state);
        RouteCandidateReconciliation result = state.routeState().reconcileRouteCandidate(
                candidateChild,
                subTasksEqual,
                replacementApplied,
                activeChildAfter
        );
        if (activeChildBefore != null
                && activeChildBefore != activeChildAfter
                && replacementApplied) {
            automaticTerminals.recordScopeIdentity(
                    state.automaticContext(),
                    "ROUTE_CHILD",
                    routeChildId(operationId, previousRouteChildLifecycleSequence),
                    activeChildBefore,
                    StoreDepositOperationContext.identity(parent),
                    StoreDepositOperationContext.identity(activeChildBefore),
                    "GENERIC_TASK_RECONCILIATION",
                    previousRouteCorrelation
            );
        }
        return result;
    }

    public void expectActiveRouteChildTerminal(StoreDepositOperationState state,
                                               String reconciliationRole,
                                               boolean replacementApplied,
                                               Task activeChildAfter) {
        if (!isAutomatic(state)
                || !"ROOT_ROUTE".equals(reconciliationRole)
                || !replacementApplied
                || activeChildAfter == null) {
            return;
        }
        automaticTerminals.expectScopeIdentity(
                state.automaticContext(),
                "ROUTE_CHILD",
                routeChildId(
                        state.context().operationId(),
                        state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                )
        );
    }

    public void recordRouteReconciliationTerminal(StoreDepositOperationState state,
                                                  Task parent,
                                                  Task candidateChild,
                                                  RouteCandidateReconciliation result) {
        if (!isAutomatic(state) || result == null || !result.available()) {
            return;
        }
        String operationId = state.context().operationId();
        String selectedCandidateGenerationId = candidateId(
                operationId,
                result.stagedCandidateGeneration()
        );
        automaticTerminals.recordScopeIdentity(
                state.automaticContext(),
                "ROUTE_RECONCILIATION",
                selectedCandidateGenerationId,
                candidateChild,
                StoreDepositOperationContext.identity(parent),
                StoreDepositOperationContext.identity(candidateChild),
                result.outcome(),
                StoreDepositEventFields.merge(
                        StoreDepositEventFields.operationFields(state),
                        new Object[]{
                                "selectedCandidateGenerationId", selectedCandidateGenerationId
                        }
                )
        );
    }

    public Object[] reconciliationFields(StoreDepositOperationState state,
                                         RouteCandidateReconciliation result,
                                         Task activeChildBefore,
                                         Task candidateChild,
                                         Task activeChildAfter) {
        if (!isAutomatic(state) || result == null || !result.available()) {
            return new Object[0];
        }
        String operationId = state.context().operationId();
        return new Object[]{
                "routeCandidateReconciliationAvailable", true,
                "routeCandidateReconciliationOutcome", result.outcome(),
                "selectedCandidateGenerationId", candidateId(
                        operationId,
                        result.stagedCandidateGeneration()
                ),
                "activeSelectedCandidateGenerationId", candidateId(
                        operationId,
                        result.activeCandidateGeneration()
                ),
                "candidateBehaviorGenerationId", result.behaviorGenerationId(),
                "activeRouteChildBefore", StoreDepositOperationContext.identity(activeChildBefore),
                "activeMoveItemChildBefore", "UNAVAILABLE",
                "candidateNextChild", StoreDepositOperationContext.identity(candidateChild),
                "candidateNextDestination", ChatClefDiagnostics.blockPos(result.selectedTarget()),
                "activeChildAfterReconciliation", StoreDepositOperationContext.identity(activeChildAfter),
                "nextBranch", state.routeState().currentBranch(),
                "routeReconciliationObservationComplete", false,
                "routeReconciliationMissingBoundaries", "ACTIVE_MOVE_ITEM_CHILD",
                "behavior_effect", "none"
        };
    }

    public void observeTaskLifecycle(Task task, String action, String phase) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || !"STOP".equals(action)
                || !"BEGIN".equals(phase)) {
            return;
        }
        StoreDepositOperationState state = bindings.stateFor(task);
        if (!isAutomatic(state) || !state.routeState().isCurrentRouteChild(task)) {
            return;
        }
        String operationId = state.context().operationId();
        automaticTerminals.recordScopeIdentity(
                state.automaticContext(),
                "ROUTE_CHILD",
                routeChildId(
                        operationId,
                        state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                ),
                task,
                state.context().rootTaskIdentity(),
                StoreDepositOperationContext.identity(task),
                "TASK_STOP_BEGIN",
                StoreDepositEventFields.activeRouteIdentityFields(state)
        );
    }

    private static boolean isAutomatic(StoreDepositOperationState state) {
        return state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && state.automaticContext().available();
    }

    private static String candidateId(String operationId, long generation) {
        return generation <= 0 ? "UNAVAILABLE" : operationId + "-candidate-" + generation;
    }

    private static String routeChildId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-route-child-" + sequence;
    }
}
