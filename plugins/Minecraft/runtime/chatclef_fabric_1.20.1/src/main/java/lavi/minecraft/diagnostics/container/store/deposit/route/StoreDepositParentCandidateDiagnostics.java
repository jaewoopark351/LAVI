package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerParentDecision;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Keep parent-candidate recording and emission in one route-focused collaborator.
public final class StoreDepositParentCandidateDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositCheckpointDiagnostics checkpoints;

    public StoreDepositParentCandidateDiagnostics(StoreDepositBindingRegistry bindings,
                                                   StoreDepositEmissionGate emissionGate,
                                                   StoreDepositCheckpointDiagnostics checkpoints) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.checkpoints = checkpoints;
    }

    public void logParentCandidateDecision(Task task,
                                           String selectedBranch,
                                           BlockPos rawClosest,
                                           boolean closestWithinRange,
                                           boolean currentTryWithinExtraRange,
                                           BlockPos currentChestTry,
                                           ItemTarget[] notStored,
                                           Object[] fields) {
        logDecision(
                task,
                selectedBranch,
                true,
                rawClosest,
                rawClosest != null,
                closestWithinRange,
                true,
                currentTryWithinExtraRange,
                currentChestTry,
                notStored,
                fields
        );
    }

    public void logDepositAllParentCandidateDecision(Task task,
                                                     String selectedBranch,
                                                     boolean closestEvaluated,
                                                     BlockPos rawClosest,
                                                     boolean closestWithinRangeEvaluated,
                                                     boolean closestWithinRange,
                                                     boolean currentTryWithinExtraRangeEvaluated,
                                                     boolean currentTryWithinExtraRange,
                                                     BlockPos currentChestTry,
                                                     ItemTarget[] notStored,
                                                     Object[] fields) {
        logDecision(
                task,
                selectedBranch,
                closestEvaluated,
                rawClosest,
                closestWithinRangeEvaluated,
                closestWithinRange,
                currentTryWithinExtraRangeEvaluated,
                currentTryWithinExtraRange,
                currentChestTry,
                notStored,
                fields
        );
    }

    private void logDecision(Task task,
                             String selectedBranch,
                             boolean closestEvaluated,
                             BlockPos rawClosest,
                             boolean closestWithinRangeEvaluated,
                             boolean closestWithinRange,
                             boolean currentTryWithinExtraRangeEvaluated,
                             boolean currentTryWithinExtraRange,
                             BlockPos currentChestTry,
                             ItemTarget[] notStored,
                             Object[] fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(task);
            if (state == null) {
                return;
            }
            state.recordParentCandidateDecision(selectedBranch);
            if (!state.context().isDepositAllOperation()) {
                String legacyKey = StoreDepositEventFields.operationId(state)
                        + "|" + selectedBranch
                        + "|" + ChatClefDiagnostics.blockPos(rawClosest)
                        + "|" + ChatClefDiagnostics.blockPos(currentChestTry);
                if (!emissionGate.shouldEmitDetail(
                        StoreDepositEventFields.operationId(state),
                        "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                        legacyKey
                )) {
                    return;
                }
                StoreDepositBoundedEventLogger.log(
                        "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                        "store_container_parent_candidate_decision",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                StoreDepositEventFields.parentCandidateDecisionFields(
                                        state,
                                        task,
                                        selectedBranch,
                                        rawClosest,
                                        closestWithinRange,
                                        currentTryWithinExtraRange,
                                        currentChestTry,
                                        notStored,
                                        fields
                                )
                        )
                );
                return;
            }
            Object rangeDecisionOverride = fieldValue(fields, "candidateDecisionOutcome");
            String rangeDecisionOutcome = "unavailable".equals(rangeDecisionOverride)
                    ? rangeDecisionOutcome(
                            closestEvaluated,
                            rawClosest,
                            closestWithinRange,
                            currentTryWithinExtraRange
                    )
                    : String.valueOf(rangeDecisionOverride);
            StoreContainerParentDecision decision = state.routeState().recordParentDecision(
                    selectedBranch,
                    closestEvaluated,
                    rawClosest,
                    closestWithinRangeEvaluated,
                    closestWithinRange,
                    currentTryWithinExtraRangeEvaluated,
                    currentTryWithinExtraRange,
                    currentChestTry,
                    rangeDecisionOutcome,
                    notStoredStateHash(notStored),
                    null
            );
            String operationId = StoreDepositEventFields.operationId(state);
            String key = operationId
                    + "|" + selectedBranch
                    + "|" + ChatClefDiagnostics.blockPos(rawClosest)
                    + "|" + ChatClefDiagnostics.blockPos(currentChestTry)
                    + "|" + rangeDecisionOutcome
                    + "|" + decision.notStoredStateHash();
            if (emissionGate.shouldEmitDetail(
                    operationId,
                    "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                    key
            )) {
                Object[] eventFields = StoreContainerCandidateEventFields.parentDecisionFields(
                        state,
                        task,
                        decision,
                        notStored,
                        withoutField(fields, "fallbackContainerItemPresent")
                );
                eventFields = StoreDepositEventFields.merge(
                        eventFields,
                        StoreContainerRangeEventFields.parentDecisionFields(
                                state.routeState().currentRangeTransition(),
                                fieldValue(fields, "fallbackContainerItemPresent")
                        )
                );
                StoreDepositBoundedEventLogger.log(
                        "STORE_CONTAINER_PARENT_CANDIDATE_DECISION",
                        "store_container_parent_candidate_decision",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(eventFields)
                );
            }
            checkpoints.emitIfDue(task, state);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static String rangeDecisionOutcome(boolean closestEvaluated,
                                               BlockPos rawClosest,
                                               boolean closestWithinRange,
                                               boolean currentTryWithinExtraRange) {
        if (!closestEvaluated) {
            return "NOT_EVALUATED_EARLY_GET_MISSING_TARGET";
        }
        if (rawClosest == null) {
            return "NO_RAW_CLOSEST";
        }
        if (closestWithinRange && currentTryWithinExtraRange) {
            return "BOTH_RANGE_CONDITIONS";
        }
        if (closestWithinRange) {
            return "RAW_CLOSEST_WITHIN_50";
        }
        if (currentTryWithinExtraRange) {
            return "CURRENT_TRY_WITHIN_70";
        }
        return "RAW_PRESENT_BUT_OUTSIDE_RANGES";
    }

    private static String notStoredStateHash(ItemTarget[] notStored) {
        return Integer.toHexString(ChatClefDiagnostics.itemTargets(notStored).hashCode());
    }

    private static Object fieldValue(Object[] fields, String key) {
        if (fields == null || key == null) {
            return "unavailable";
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return fields[index + 1];
            }
        }
        return "unavailable";
    }

    private static Object[] withoutField(Object[] fields, String key) {
        if (fields == null || fields.length == 0 || key == null) {
            return fields;
        }
        int retainedLength = 0;
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (!key.equals(String.valueOf(fields[index]))) {
                retainedLength += 2;
            }
        }
        Object[] retained = new Object[retainedLength];
        int targetIndex = 0;
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (!key.equals(String.valueOf(fields[index]))) {
                retained[targetIndex++] = fields[index];
                retained[targetIndex++] = fields[index + 1];
            }
        }
        return retained;
    }
}
