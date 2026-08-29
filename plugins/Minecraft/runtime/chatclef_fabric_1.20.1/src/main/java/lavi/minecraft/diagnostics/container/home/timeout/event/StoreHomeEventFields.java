package lavi.minecraft.diagnostics.container.home.timeout.event;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressState;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressObservation;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressFingerprint;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;

//20260828_kpopmodder: Format STORE_HOME diagnostic DTOs without deciding Task behavior or emission.
public final class StoreHomeEventFields {
    private StoreHomeEventFields() {
    }

    public static Object[] operation(
            Task topLevelTask,
            String topLevelTaskRunId,
            StoreHomePhase phase,
            long clientTickId,
            long operationStartClientTickId,
            boolean operationStartClientTickKnown,
            StoreHomeTimeoutObservation timeout) {
        return new Object[]{
                "operationStartClientTickId",
                operationStartClientTickKnown
                        ? operationStartClientTickId
                        : "unavailable_first_visible_boundary_is_not_actual_start",
                "operationFirstObservedClientTickId", operationStartClientTickId,
                "operationStartClientTickSource",
                operationStartClientTickKnown
                        ? "TASK_ON_START"
                        : "FIRST_VISIBLE_BOUNDARY_NOT_ACTUAL_START",
                "elapsedOperationClientTicks",
                operationStartClientTickKnown
                        ? elapsed(clientTickId, operationStartClientTickId)
                        : "unavailable_first_visible_boundary_is_not_actual_start",
                "operationTicks", timeout.operationNoProgressTicks(),
                "operationTicksMeaning", "OPERATION_NO_PROGRESS_TICKS",
                "maxOperationTicks", timeout.maxOperationNoProgressTicks(),
                "maxOperationTicksMeaning", "OPERATION_NO_PROGRESS_LIMIT",
                "maxCandidateTicks", timeout.activeCandidateLimitTicks(),
                "maxCandidateTicksMeaning", "ACTIVE_CANDIDATE_PHASE_LIMIT",
                "timeoutPolicyVersion", "store_home_phase_scoped_v1",
                "operationNoProgressTicks", timeout.operationNoProgressTicks(),
                "maxOperationNoProgressTicks",
                timeout.maxOperationNoProgressTicks(),
                "operationEmergencyElapsedTicks", timeout.operationActiveTicks(),
                "maxOperationEmergencyHardCapTicks",
                timeout.maxOperationEmergencyHardCapTicks(),
                "candidateNavigationNoProgressTicks",
                timeout.candidateNavigationNoProgressTicks(),
                "candidateActiveTicks", timeout.candidateActiveTicks(),
                "maxCandidateNavigationNoProgressTicks",
                timeout.maxCandidateNavigationNoProgressTicks(),
                "candidateLocalInteractionTicks",
                timeout.candidateLocalInteractionTicks(),
                "maxCandidateLocalInteractionTicks",
                timeout.maxCandidateLocalInteractionTicks(),
                "candidateTimeoutPhase", timeout.candidateTimeoutPhase(),
                "candidateLocalInteractionStarted",
                timeout.candidateLocalInteractionStarted(),
                "movementJitterThresholdBlocks", timeout.movementJitterBlocks(),
                "bestDistanceImprovementEpsilonBlocks",
                timeout.bestDistanceImprovementEpsilonBlocks(),
                "behaviorProgressDistanceMetric",
                "player_vec3d_to_candidate_block_center_3d_euclidean_blocks_v1",
                "timeoutClockBasis", "STORE_HOME_ACTIVE_ROOT_ON_TICK",
                "phase", phase == null ? "unavailable" : phase.name(),
                "topLevelTask", ChatClefDiagnostics.className(topLevelTask),
                "topLevelTaskRunId", topLevelTaskRunId,
                "parentTask", ChatClefDiagnostics.className(topLevelTask),
                "storeHomeParentTaskRunId", topLevelTaskRunId,
                "parentTaskMeaning", "STORE_HOME_TOP_LEVEL_OWNER",
                "timeoutEvaluationOrder",
                "EMERGENCY_BEFORE_BEHAVIOR_NO_PROGRESS_CHECKPOINTED_THEN_CANDIDATE"
        };
    }

    public static Object[] operationContext(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate) {
        String worldKey = context == null
                ? candidate == null ? "unavailable" : candidate.destination().worldKey()
                : context.worldKey();
        String dimension = context == null
                ? candidate == null
                ? "unavailable"
                : candidate.destination().dimension().name()
                : context.dimension().name();
        return new Object[]{
                "worldKey", worldKey,
                "dimension", dimension
        };
    }

    public static Object[] candidate(
            StoreHomeCandidateProgressState state,
            int remainingCandidateCountIncludingCurrent,
            int candidateTicks,
            long clientTickId) {
        return candidate(
                state,
                remainingCandidateCountIncludingCurrent,
                true,
                candidateTicks,
                clientTickId
        );
    }

    public static Object[] candidateAfterRejection(
            StoreHomeCandidateProgressState state,
            int remainingCandidateCountAfterRejection,
            int candidateTicks,
            long clientTickId) {
        return candidate(
                state,
                remainingCandidateCountAfterRejection,
                false,
                candidateTicks,
                clientTickId
        );
    }

    private static Object[] candidate(
            StoreHomeCandidateProgressState state,
            int queueRemaining,
            boolean candidateIncludedInQueue,
            int candidateTicks,
            long clientTickId) {
        if (state == null) {
            return candidateUnavailable(
                    0, queueRemaining
            );
        }
        int normalizedQueueRemaining = Math.max(0, queueRemaining);
        AutoDepositTrustedDestinationCandidate candidate = state.candidate();
        return new Object[]{
                "candidateId", state.candidateId(),
                "candidateAttemptId", state.candidateAttemptId(),
                "destinationId", candidate.destinationId(),
                "destinationCanonicalKey", candidate.destination().key(),
                "candidatePosition", candidate.position().toShortString(),
                "candidateOrdinal", state.candidateOrdinal(),
                "candidateAttemptOrdinal", state.candidateAttemptOrdinal(),
                "candidateCount", state.totalCandidateCount(),
                "candidateQueueRemaining", normalizedQueueRemaining,
                "candidateIncludedInQueueAtEvent", candidateIncludedInQueue,
                "remainingCandidateCountIncludingCurrent",
                candidateIncludedInQueue
                        ? normalizedQueueRemaining
                        : "unavailable_candidate_already_removed",
                "remainingCandidateCountAfterCurrent",
                candidateIncludedInQueue
                        ? Math.max(0, normalizedQueueRemaining - 1)
                        : normalizedQueueRemaining,
                "candidateStartClientTickId", state.candidateStartClientTickIdValue(),
                "candidateFirstObservedClientTickId",
                state.candidateStartClientTickId(),
                "candidateStartClientTickSource",
                state.candidateStartClientTickSource(),
                "elapsedCandidateClientTicks",
                state.elapsedCandidateClientTicks(clientTickId),
                "candidateTicks", candidateTicks,
                "candidateTicksMeaning", "ACTIVE_CANDIDATE_PHASE_TIMEOUT_TICKS",
                "childTaskClass", state.childTaskClass(),
                "childTaskRunId", state.childTaskRunId(),
                "childTaskRunOrdinal", state.childTaskRunOrdinal()
        };
    }

    public static Object[] candidateBeforeAttempt(
            long operationId,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateOrdinal,
            int candidateCount,
            int remainingCandidateCountAfterRejection) {
        return new Object[]{
                "candidateId", "store-home-operation-" + operationId
                + "-candidate-" + Math.max(1, candidateOrdinal),
                "candidateAttemptId", "unavailable_not_started",
                "destinationId", candidate.destinationId(),
                "destinationCanonicalKey", candidate.destination().key(),
                "candidatePosition", candidate.position().toShortString(),
                "candidateOrdinal", Math.max(1, candidateOrdinal),
                "candidateAttemptOrdinal", "unavailable_not_started",
                "candidateCount", Math.max(0, candidateCount),
                "candidateQueueRemaining",
                Math.max(0, remainingCandidateCountAfterRejection),
                "candidateIncludedInQueueAtEvent", false,
                "remainingCandidateCountIncludingCurrent",
                "unavailable_candidate_already_removed",
                "remainingCandidateCountAfterCurrent",
                Math.max(0, remainingCandidateCountAfterRejection),
                "candidateStartClientTickId", "unavailable_not_started",
                "candidateStartClientTickSource", "CANDIDATE_ATTEMPT_NOT_STARTED",
                "elapsedCandidateClientTicks", "unavailable_not_started",
                "candidateTicks", "unavailable_not_started",
                "childTaskClass", "none",
                "childTaskRunId", "none",
                "childTaskRunOrdinal", 0
        };
    }

    public static Object[] candidateAfterUnobservedAttempt(
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateCount,
            int remainingCandidateCountAfterRejection,
            int candidateTicks) {
        return new Object[]{
                "candidateId", "unavailable_attempt_identity_not_observed",
                "candidateAttemptId", "unavailable_attempt_identity_not_observed",
                "destinationId", candidate.destinationId(),
                "destinationCanonicalKey", candidate.destination().key(),
                "candidatePosition", candidate.position().toShortString(),
                "candidateOrdinal", "unavailable_attempt_identity_not_observed",
                "candidateAttemptOrdinal",
                "unavailable_attempt_identity_not_observed",
                "candidateCount", Math.max(0, candidateCount),
                "candidateQueueRemaining",
                Math.max(0, remainingCandidateCountAfterRejection),
                "candidateIncludedInQueueAtEvent", false,
                "remainingCandidateCountIncludingCurrent",
                "unavailable_candidate_already_removed",
                "remainingCandidateCountAfterCurrent",
                Math.max(0, remainingCandidateCountAfterRejection),
                "candidateStartClientTickId", "unavailable_not_observed",
                "candidateStartClientTickSource", "ATTEMPT_START_NOT_OBSERVED",
                "elapsedCandidateClientTicks", "unavailable_not_observed",
                "candidateTicks", Math.max(0, candidateTicks),
                "childTaskClass", "unavailable_not_observed",
                "childTaskRunId", "unavailable_not_observed",
                "childTaskRunOrdinal", "unavailable_not_observed",
                "candidateObservationFallback",
                "ACTIVE_ATTEMPT_REJECTION_WITHOUT_PRIOR_DIAGNOSTIC_STATE"
        };
    }

    public static Object[] progress(
            StoreHomeCandidateProgressState state,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation observation,
            long clientTickId,
            int suppressedRepeatCount) {
        return new Object[]{
                "progressKind", observation.progressKind(),
                "semanticFingerprint", observation.fingerprint(),
                "semanticStateChanged", observation.semanticStateChanged(),
                "periodicSampleDue", observation.sampleDue(),
                "suppressedRepeatCountBeforeEmission", suppressedRepeatCount,
                "suppressedProgressSummaryCount", suppressedRepeatCount,
                "playerStartPosition", state.playerStartPosition(),
                "playerStartPositionSource", state.candidateStartObservationSource(),
                "playerCurrentPosition", snapshot.playerPosition(),
                "distanceMetricVersion", "block_pos_3d_squared_v1",
                "initialDistanceSquared3d", state.initialDistanceSquared3dValue(),
                "currentDistanceSquared3d", state.currentDistanceSquared3dValue(),
                "bestDistanceSquared3d", state.bestDistanceSquared3dValue(),
                "coarseDistanceBucket",
                StoreHomeProgressFingerprint.coarseDistanceBucket(
                        snapshot.currentDistanceSquared3d()
                ),
                "lastMovementClientTickId", state.lastMovementClientTickId(),
                "lastPlayerMoveClientTickId", state.lastMovementClientTickId(),
                "lastMoveTick", state.lastMovementClientTickId(),
                "lastDistanceImprovementClientTickId",
                state.lastDistanceImprovementClientTickId(),
                "lastBestDistanceImprovementClientTickId",
                state.lastDistanceImprovementClientTickId(),
                "lastDistanceImprovementTick",
                state.lastDistanceImprovementClientTickId(),
                "ticksSinceLastMovement", state.ticksSinceLastMovement(clientTickId),
                "ticksSincePlayerMove", state.ticksSinceLastMovement(clientTickId),
                "noMovementTicks", state.noMovementTicks(clientTickId),
                "ticksSinceLastDistanceImprovement",
                state.ticksSinceLastDistanceImprovement(clientTickId),
                "ticksSinceBestDistanceImprovement",
                state.ticksSinceLastDistanceImprovement(clientTickId),
                "baritonePathingActive", snapshot.baritonePathingActive(),
                "baritoneCalculationActive",
                calculationActive(snapshot.baritoneCalculationState()),
                "baritoneCalculationState", snapshot.baritoneCalculationState(),
                "baritonePathPresent", snapshot.baritonePathPresent(),
                "customGoalActive", snapshot.customGoalActive(),
                "normalizedGoalType", snapshot.normalizedGoalType(),
                "normalizedGoalTarget", snapshot.normalizedGoalTarget(),
                "normalizedGoalTargetObservationStatus",
                "UNAVAILABLE_WITHOUT_OPAQUE_OBJECT_STRING_OR_REFLECTION",
                "expectedCandidatePosition",
                state.candidate().position().toShortString(),
                "goalMatchesCandidatePosition",
                snapshot.goalMatchesCandidatePosition(),
                "screenName", snapshot.screenClass(),
                "screenClass", snapshot.screenClass(),
                "handlerName", snapshot.handlerClass(),
                "handlerClass", snapshot.handlerClass(),
                "syncId", snapshot.syncId(),
                "exactBindingMatched", snapshot.exactBindingMatched(),
                "exactBindingEverObserved", state.exactBindingEverObserved(),
                "supportedHandlerEverObserved",
                state.supportedHandlerEverObserved(),
                "containerSessionState", snapshot.containerSessionState(),
                "observedContainerSessionOrdinal",
                snapshot.containerSessionOrdinal(),
                "observedManifestRevision", snapshot.planRevision(),
                "sessionPendingTransfer", snapshot.sessionPendingTransfer(),
                "executorPendingTransfer", snapshot.executorPendingTransfer(),
                "pathingExceptionCount",
                "unavailable_without_engine_exception_interception",
                "pathingObservationUnavailableCount",
                state.pathingObservationUnavailableCount(),
                "snapshotCaptureStatus", snapshot.captureStatus(),
                "snapshotCaptureErrors", snapshot.captureErrors(),
                "diagnosticCaptureStatus", snapshot.captureStatus(),
                "diagnosticErrorClass",
                "unavailable_without_engine_exception_interception"
        };
    }

    public static Object[] session(HomeStorageContainerSession session) {
        if (session == null) {
            return new Object[]{
                    "containerSessionActive", false,
                    "containerSessionOrdinal", "unavailable",
                    "manifestRevision", "unavailable",
                    "manifestActive", false,
                    "pendingTransfer", false,
                    "activeContainerSessionOrdinal", "unavailable",
                    "activePlanRevision", "unavailable",
                    "activeSessionPendingTransfer", false
            };
        }
        return new Object[]{
                "containerSessionActive", true,
                "containerSessionOrdinal", session.ordinal(),
                "manifestRevision", session.plan().revision(),
                "manifestActive", true,
                "pendingTransfer", session.pendingTransfer().isPresent(),
                "activeContainerSessionOrdinal", session.ordinal(),
                "activePlanRevision", session.plan().revision(),
                "activeSessionPendingTransfer", session.pendingTransfer().isPresent(),
                "activeSessionRemainingStacks",
                session.progress().remainingStackCount()
        };
    }

    public static Object[] timeoutDecision(
            String timeoutScope,
            long decisionClientTickId,
            Object candidateTicksBeforeDecision,
            int operationTicksBeforeDecision,
            boolean candidateTimeoutEvaluated,
            Object candidateTimeoutConditionMatched,
            boolean operationTimeoutEvaluated,
            boolean operationTimeoutConditionMatched,
            String plannedTimeoutAction,
            String plannedRejectionReason,
            String plannedTerminalReason,
            String plannedTerminalResult,
            boolean executorPendingAtDecision,
            boolean sessionPendingAtDecision) {
        return new Object[]{
                "timeoutScope", timeoutScope,
                "decisionClientTickId", decisionClientTickId,
                "candidateTicksBeforeDecision", candidateTicksBeforeDecision,
                "operationTicksBeforeDecision", operationTicksBeforeDecision,
                "candidateTimeoutEvaluated", candidateTimeoutEvaluated,
                "candidateTimeoutConditionMatched",
                candidateTimeoutConditionMatched,
                "operationTimeoutEvaluated", operationTimeoutEvaluated,
                "operationTimeoutConditionMatched",
                operationTimeoutConditionMatched,
                "timeoutComparisonOperator", "GREATER_THAN_OR_EQUAL",
                "plannedTimeoutAction", plannedTimeoutAction,
                "plannedRejectionReason", plannedRejectionReason,
                "plannedTerminalReason", plannedTerminalReason,
                "plannedTerminalResult", plannedTerminalResult,
                "pendingTransferAtDecision",
                executorPendingAtDecision || sessionPendingAtDecision,
                "executorPendingAtDecision", executorPendingAtDecision,
                "sessionPendingAtDecision", sessionPendingAtDecision,
                "capturedBeforeDecisionSideEffects", true
        };
    }

    public static Object[] terminal(
            StoreHomeResult result,
            String reason,
            StoreHomeOperationProgress operation) {
        return new Object[]{
                "terminalResult", result == null ? "unavailable" : result.name(),
                "operationResult", result == null ? "unavailable" : result.name(),
                "terminalReason", reason,
                "storedItems", operation.storedItems(),
                "touchedStackCount", operation.touchedStackCount(),
                "remainingStackCount", operation.latestRemainingStacks(),
                "capacityFailureCount", operation.capacityFailures(),
                "unavailableFailureCount", operation.unavailableFailures(),
                "taskBehaviorAffectedByDiagnostics", false
        };
    }

    public static Object[] candidateUnavailable(
            Object candidateCount,
            Object remainingCandidateCountIncludingCurrent) {
        Object normalizedCandidateCount = normalizedCount(candidateCount);
        Object remaining = normalizedCount(remainingCandidateCountIncludingCurrent);
        return new Object[]{
                "candidateId", "unavailable",
                "candidateAttemptId", "unavailable",
                "destinationId", "unavailable",
                "candidatePosition", "unavailable",
                "candidateOrdinal", "unavailable",
                "candidateAttemptOrdinal", "unavailable",
                "candidateCount", normalizedCandidateCount,
                "candidateQueueRemaining", remaining,
                "remainingCandidateCountIncludingCurrent", remaining,
                "remainingCandidateCountAfterCurrent", remaining,
                "candidateStartClientTickId", "unavailable",
                "candidateStartClientTickSource", "NO_ACTIVE_CANDIDATE",
                "elapsedCandidateClientTicks", "unavailable",
                "candidateTicks", "unavailable",
                "childTaskClass", "none",
                "childTaskRunId", "none",
                "childTaskRunOrdinal", 0
        };
    }

    private static Object normalizedCount(Object value) {
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        return value == null ? "unavailable" : value;
    }

    private static Object calculationActive(String state) {
        if ("CALCULATION_IN_PROGRESS".equals(state)) {
            return true;
        }
        return "UNAVAILABLE".equals(state) ? "unavailable" : false;
    }

    private static long elapsed(long current, long start) {
        return current >= start ? current - start : 0L;
    }
}
