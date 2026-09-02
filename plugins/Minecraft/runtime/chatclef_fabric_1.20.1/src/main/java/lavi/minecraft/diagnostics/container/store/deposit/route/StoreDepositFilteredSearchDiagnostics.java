package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateCollector;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateObservation;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateRejectionReason;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260902_kpopmodder: Isolate filtered-search scope consumption and its single bounded result event family.
public final class StoreDepositFilteredSearchDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositFilteredSearchDiagnostics(StoreDepositBindingRegistry bindings,
                                                  StoreDepositEmissionGate emissionGate) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
    }

    public void begin(Task task) {
        try {
            StoreContainerCandidateCollector.begin(bindings.stateFor(task), task);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void beginParentSelection(Task task, BlockPos rawClosest) {
        try {
            StoreContainerCandidateCollector.beginParentSelection(
                    bindings.stateFor(task),
                    task,
                    rawClosest
            );
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void observeContainerEligibility(BlockPos position, String outcome) {
        StoreContainerCandidateRejectionReason reason;
        try {
            reason = StoreContainerCandidateRejectionReason.valueOf(outcome);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            reason = StoreContainerCandidateRejectionReason.UNKNOWN;
        }
        StoreContainerCandidateCollector.observe(position, reason);
    }

    public void endParentSelection(Task task, boolean completedNormally) {
        try {
            StoreContainerCandidateCollector.end(task, completedNormally);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void end(Task task, boolean completedNormally, Block[] targetBlocks) {
        try {
            StoreContainerCandidateCollector.end(task, completedNormally);
            if (!completedNormally) {
                logIncompleteResult(task, targetBlocks);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public void logResult(Task task, Optional<BlockPos> result, Block[] targetBlocks) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            String relation = result != null && result.isPresent()
                    ? "FILTERED_TARGET_PRESENT"
                    : "FILTERED_TARGET_ABSENT";
            state.recordFilteredSearchResult(relation);
            boolean depositAllParentSelectionSearch = state.context().isDepositAllOperation()
                    && state.context().isRoot(task);
            boolean depositAllContainerRouteSearch = state.context().isDepositAllOperation()
                    && "OPEN_EXISTING".equals(state.routeState().currentBranch())
                    && state.routeState().isCurrentRouteChild(task);
            boolean depositAllCandidateSearch = depositAllParentSelectionSearch || depositAllContainerRouteSearch;
            StoreContainerCandidateObservation observation = depositAllCandidateSearch
                    ? StoreContainerCandidateCollector.take(state, task)
                    : StoreContainerCandidateObservation.unavailable();
            if (depositAllCandidateSearch) {
                state.routeState().recordFilteredSearch(result, observation);
            }
            String position = result == null
                    ? "unavailable"
                    : result.map(BlockPos::toShortString).orElse("none");
            BlockPos originatingRaw = observation.available()
                    ? observation.parentRawClosest()
                    : state.routeState().currentParentDecision().rawClosest();
            String operationId = StoreDepositEventFields.operationId(state);
            String key = depositAllCandidateSearch
                    ? operationId
                            + "|" + relation
                            + "|" + ChatClefDiagnostics.blockPos(originatingRaw)
                            + "|" + position
                            + "|" + observation.candidateEvaluationCount()
                            + "|" + observation.rejectionCountsByReason()
                            + "|" + observation.firstRejectedReason()
                            + "|" + observation.lastRejectedReason()
                            + "|" + observation.rawCandidatePredicateOutcome()
                            + "|" + observation.rawCandidateRejectionReason()
                    : operationId
                            + "|" + StoreDepositEventFields.identity(task)
                            + "|" + relation
                            + "|" + position;
            if (!emissionGate.shouldEmitDetail(
                    operationId,
                    "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                    key
            )) {
                return;
            }
            Object[] eventFields = depositAllCandidateSearch
                    ? StoreContainerCandidateEventFields.filteredSearchFields(
                            state,
                            task,
                            result,
                            targetBlocks,
                            observation
                    )
                    : StoreDepositEventFields.filteredSearchResultFields(
                            state,
                            task,
                            result,
                            targetBlocks
                    );
            StoreDepositBoundedEventLogger.log(
                    "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                    "store_container_filtered_search_result",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(eventFields)
            );
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private void logIncompleteResult(Task task, Block[] targetBlocks) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositOperationState state = bindings.stateFor(task);
        if (state == null
                || !state.context().isDepositAllOperation()
                || !"OPEN_EXISTING".equals(state.routeState().currentBranch())
                || !state.routeState().isCurrentRouteChild(task)) {
            return;
        }
        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, task);
        if (!observation.available()) {
            return;
        }
        state.recordFilteredSearchResult("FILTERED_SCAN_DID_NOT_COMPLETE");
        state.routeState().recordFilteredSearch(Optional.empty(), observation);
        String operationId = StoreDepositEventFields.operationId(state);
        String key = operationId
                + "|FILTERED_SCAN_DID_NOT_COMPLETE|"
                + ChatClefDiagnostics.blockPos(observation.parentRawClosest())
                + "|" + observation.rawCandidatePredicateOutcome()
                + "|" + observation.rawCandidateRejectionReason();
        if (!emissionGate.shouldEmitDetail(
                operationId,
                "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                key
        )) {
            return;
        }
        StoreDepositBoundedEventLogger.log(
                "STORE_CONTAINER_FILTERED_SEARCH_RESULT",
                "store_container_filtered_scan_did_not_complete",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        StoreContainerCandidateEventFields.filteredSearchFields(
                                state,
                                task,
                                Optional.empty(),
                                targetBlocks,
                                observation
                        )
                )
        );
    }
}
