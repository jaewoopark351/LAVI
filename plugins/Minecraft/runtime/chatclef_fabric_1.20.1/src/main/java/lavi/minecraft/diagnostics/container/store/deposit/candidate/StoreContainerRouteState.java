package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeAggregate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeTracker;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeTransition;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class StoreContainerRouteState {
    private static final long CHECKPOINT_INTERVAL_TICKS = 1200;
    private static final int MAX_CHECKPOINTS = 64;

    private final long startTick;
    private final StoreContainerRangeTracker rangeTracker = new StoreContainerRangeTracker();
    private StoreContainerParentDecision currentParentDecision = StoreContainerParentDecision.unavailable();
    private final Map<String, Integer> branchCounts = new LinkedHashMap<>();
    private final Map<String, Integer> branchTransitionCounts = new LinkedHashMap<>();
    private final Map<String, Integer> branchCountsSinceCheckpoint = new LinkedHashMap<>();
    private final Map<String, Integer> branchTransitionCountsSinceCheckpoint = new LinkedHashMap<>();
    private final Map<StoreContainerCandidateRejectionReason, Integer> rejectionCounts =
            new EnumMap<>(StoreContainerCandidateRejectionReason.class);
    private final Map<StoreContainerCandidateRejectionReason, Integer> rejectionCountsSinceCheckpoint =
            new EnumMap<>(StoreContainerCandidateRejectionReason.class);
    private long candidateDecisionSequence;
    private long branchEpoch;
    private long semanticVersion;
    private long lastCheckpointSemanticVersion;
    private long lastCheckpointTick;
    private long checkpointSequence;
    private int checkpointCount;
    private BlockPos currentFilteredCandidate;
    private BlockPos currentPursuit;
    private String currentPursuitAction = "NONE";
    private String notStoredStateHash = "unavailable";
    private int childReplacementCount;
    private int childReplacementCountSinceCheckpoint;
    private int rootRouteChildReplacementCount;
    private long childLifecycleSequence;
    private Object currentRouteChild;
    private long activeStoreAttemptSequence;
    private long activeStoreAttemptCandidateDecisionSequence;
    private long activeStoreAttemptBranchEpoch;
    private String activeStoreAttemptBranch = "NONE";
    private long activeStoreAttemptRouteChildLifecycleSequence;
    private int activeStoreAttemptRouteChildReplacementCount;
    private int transferDecisionCount;
    private int transferDecisionCountSinceCheckpoint;
    private int candidateEvaluationCount;
    private int candidateEvaluationCountSinceCheckpoint;
    private int predicateAcceptedCount;
    private int predicateAcceptedCountSinceCheckpoint;
    private int predicateRejectedCount;
    private int predicateRejectedCountSinceCheckpoint;
    private String lastSuccessfulBoundary = "NONE";
    private String firstExplicitFailureBoundary = "NONE";
    private String firstUnobservedBoundary = "NONE";
    private String lastParentSemanticKey = "NONE";
    private String lastFilteredSemanticKey = "NONE";
    private String lastPursuitSemanticKey = "NONE";

    public StoreContainerRouteState(long startTick) {
        this.startTick = startTick;
        this.lastCheckpointTick = startTick;
    }

    public synchronized StoreContainerParentDecision recordParentDecision(String selectedBranch,
                                                                          boolean closestEvaluated,
                                                                          BlockPos rawClosest,
                                                                          boolean closestWithinRangeEvaluated,
                                                                          boolean closestWithinRange,
                                                                          boolean currentTryWithinExtraRangeEvaluated,
                                                                          boolean currentTryWithinExtraRange,
                                                                          BlockPos currentChestTry,
                                                                          String rangeDecisionOutcome,
                                                                          String nextNotStoredStateHash) {
        return recordParentDecision(
                selectedBranch,
                closestEvaluated,
                rawClosest,
                closestWithinRangeEvaluated,
                closestWithinRange,
                currentTryWithinExtraRangeEvaluated,
                currentTryWithinExtraRange,
                currentChestTry,
                rangeDecisionOutcome,
                nextNotStoredStateHash,
                null
        );
    }

    public synchronized StoreContainerParentDecision recordParentDecision(String selectedBranch,
                                                                          boolean closestEvaluated,
                                                                          BlockPos rawClosest,
                                                                          boolean closestWithinRangeEvaluated,
                                                                          boolean closestWithinRange,
                                                                          boolean currentTryWithinExtraRangeEvaluated,
                                                                          boolean currentTryWithinExtraRange,
                                                                          BlockPos currentChestTry,
                                                                          String rangeDecisionOutcome,
                                                                          String nextNotStoredStateHash,
                                                                          Vec3d playerPosition) {
        String normalizedBranch = normalize(selectedBranch, "UNKNOWN");
        String previousBranch = currentParentDecision.selectedBranch();
        boolean branchChanged = !normalizedBranch.equals(previousBranch);
        candidateDecisionSequence++;
        if (branchChanged) {
            branchEpoch++;
            increment(branchTransitionCounts, previousBranch + "->" + normalizedBranch, 1);
            increment(branchTransitionCountsSinceCheckpoint, previousBranch + "->" + normalizedBranch, 1);
        }
        increment(branchCounts, normalizedBranch, 1);
        increment(branchCountsSinceCheckpoint, normalizedBranch, 1);
        notStoredStateHash = normalize(nextNotStoredStateHash, "unavailable");
        String semanticKey = normalizedBranch
                + "|" + position(rawClosest)
                + "|" + closestEvaluated
                + "|" + closestWithinRangeEvaluated
                + "|" + closestWithinRange
                + "|" + currentTryWithinExtraRangeEvaluated
                + "|" + currentTryWithinExtraRange
                + "|" + position(currentChestTry)
                + "|" + normalize(rangeDecisionOutcome, "UNAVAILABLE")
                + "|" + notStoredStateHash;
        if (!semanticKey.equals(lastParentSemanticKey)) {
            semanticVersion++;
            lastParentSemanticKey = semanticKey;
        }
        currentParentDecision = new StoreContainerParentDecision(
                candidateDecisionSequence,
                branchEpoch,
                previousBranch,
                normalizedBranch,
                branchChanged,
                closestEvaluated,
                rawClosest,
                closestWithinRangeEvaluated,
                closestWithinRange,
                currentTryWithinExtraRangeEvaluated,
                currentTryWithinExtraRange,
                currentChestTry,
                normalize(rangeDecisionOutcome, "UNAVAILABLE"),
                notStoredStateHash
        );
        rangeTracker.observe(
                StoreContainerRangeSnapshot.capture(
                        playerPosition,
                        rawClosest,
                        closestWithinRangeEvaluated,
                        closestWithinRange,
                        currentChestTry,
                        currentTryWithinExtraRangeEvaluated,
                        currentTryWithinExtraRange
                ),
                normalizedBranch,
                branchEpoch,
                branchChanged
        );
        lastSuccessfulBoundary = "PARENT_CANDIDATE_DECISION";
        return currentParentDecision;
    }

    public synchronized void recordFilteredSearch(Optional<BlockPos> result,
                                                  StoreContainerCandidateObservation observation) {
        BlockPos nextFiltered = result == null ? null : result.orElse(null);
        StoreContainerCandidateObservation resolved = observation == null
                ? StoreContainerCandidateObservation.unavailable()
                : observation;
        BlockPos originatingRaw = resolved.available()
                ? resolved.parentRawClosest()
                : currentParentDecision.rawClosest();
        if ("NONE".equals(firstUnobservedBoundary)) {
            firstUnobservedBoundary = resolved.available()
                    ? "BLOCK_SCANNER_INTERNAL_FILTER_UNOBSERVED"
                    : "FILTERED_PREDICATE_DETAIL_UNAVAILABLE";
        }
        candidateEvaluationCount += resolved.candidateEvaluationCount();
        candidateEvaluationCountSinceCheckpoint += resolved.candidateEvaluationCount();
        predicateAcceptedCount += resolved.predicateAcceptedCount();
        predicateAcceptedCountSinceCheckpoint += resolved.predicateAcceptedCount();
        predicateRejectedCount += resolved.predicateRejectedCount();
        predicateRejectedCountSinceCheckpoint += resolved.predicateRejectedCount();
        addRejections(resolved);
        String semanticKey = position(nextFiltered)
                + "|" + relation(originatingRaw, nextFiltered)
                + "|" + resolved.candidateEvaluationCount()
                + "|" + resolved.predicateAcceptedCount()
                + "|" + resolved.predicateRejectedCount()
                + "|" + resolved.rejectionCountsByReason();
        if (!semanticKey.equals(lastFilteredSemanticKey)) {
            semanticVersion++;
            lastFilteredSemanticKey = semanticKey;
        }
        if (resolved.scannerCallCompletedNormally()) {
            currentFilteredCandidate = nextFiltered;
            lastSuccessfulBoundary = "FILTERED_SEARCH_RESULT";
        } else if ("NONE".equals(firstExplicitFailureBoundary)) {
            firstExplicitFailureBoundary = "FILTERED_SCAN_DID_NOT_COMPLETE";
        }
    }

    public synchronized void recordPursuit(Object candidate, String action) {
        BlockPos nextPursuit = candidate instanceof BlockPos position ? position : null;
        String normalizedAction = normalize(action, "UNKNOWN");
        String semanticKey = position(nextPursuit) + "|" + normalizedAction;
        if (!semanticKey.equals(lastPursuitSemanticKey)) {
            semanticVersion++;
            lastPursuitSemanticKey = semanticKey;
        }
        currentPursuit = nextPursuit;
        currentPursuitAction = normalizedAction;
        lastSuccessfulBoundary = "PURSUIT_DECISION";
    }

    public synchronized void recordChildReconciliation(String reconciliationRole,
                                                       Object activeChildAfter,
                                                       boolean replacementApplied) {
        recordChildReconciliation(
                reconciliationRole,
                null,
                activeChildAfter,
                replacementApplied,
                false
        );
    }

    public synchronized boolean recordChildReconciliation(String reconciliationRole,
                                                          Object activeChildBefore,
                                                          Object activeChildAfter,
                                                          boolean replacementApplied,
                                                          boolean previousChildStopCalled) {
        childLifecycleSequence++;
        if ("ROOT_ROUTE".equals(reconciliationRole)) {
            if (activeChildAfter != currentRouteChild) {
                semanticVersion++;
            }
            currentRouteChild = activeChildAfter;
            if (replacementApplied) {
                rootRouteChildReplacementCount++;
            }
            if (replacementApplied && activeChildAfter != null) {
                activeStoreAttemptSequence++;
                activeStoreAttemptCandidateDecisionSequence = currentParentDecision.sequence();
                activeStoreAttemptBranchEpoch = currentParentDecision.branchEpoch();
                activeStoreAttemptBranch = currentParentDecision.selectedBranch();
                activeStoreAttemptRouteChildLifecycleSequence = childLifecycleSequence;
                activeStoreAttemptRouteChildReplacementCount = rootRouteChildReplacementCount;
            }
        }
        if (replacementApplied) {
            childReplacementCount++;
            childReplacementCountSinceCheckpoint++;
            semanticVersion++;
        }
        boolean resourceAcquisitionInterruptedByBranchChange = "ROOT_ROUTE".equals(reconciliationRole)
                && replacementApplied
                && previousChildStopCalled
                && currentParentDecision.branchChanged()
                && "OBTAIN_CHEST".equals(currentParentDecision.previousBranch())
                && activeChildBefore != null;
        if (resourceAcquisitionInterruptedByBranchChange) {
            rangeTracker.recordResourceInterruption(currentParentDecision.sequence());
        }
        lastSuccessfulBoundary = "CHILD_RECONCILIATION";
        return resourceAcquisitionInterruptedByBranchChange;
    }

    public synchronized boolean isCurrentRouteChild(Object task) {
        return currentRouteChild != null && currentRouteChild == task;
    }

    public synchronized boolean isInCurrentRoute(Task task) {
        return task != null
                && currentRouteChild instanceof Task routeChild
                && routeChild.thisOrChildSatisfies(candidate -> candidate == task);
    }

    public synchronized void recordTransferDecision() {
        transferDecisionCount++;
        transferDecisionCountSinceCheckpoint++;
        semanticVersion++;
        lastSuccessfulBoundary = "TRANSFER_DECISION";
    }

    public synchronized Optional<StoreContainerRouteCheckpoint> checkpoint(long gameTick) {
        if (checkpointCount >= MAX_CHECKPOINTS
                || gameTick - lastCheckpointTick < CHECKPOINT_INTERVAL_TICKS
                || semanticVersion == lastCheckpointSemanticVersion) {
            return Optional.empty();
        }
        checkpointCount++;
        checkpointSequence++;
        StoreContainerRouteCheckpoint checkpoint = new StoreContainerRouteCheckpoint(
                checkpointSequence,
                gameTick,
                Math.max(0, gameTick - startTick),
                candidateDecisionSequence,
                branchEpoch,
                currentParentDecision.selectedBranch(),
                branchCountsSinceCheckpoint.toString(),
                branchTransitionCountsSinceCheckpoint.toString(),
                currentParentDecision.rawClosest(),
                currentFilteredCandidate,
                currentPursuit,
                childReplacementCountSinceCheckpoint,
                transferDecisionCountSinceCheckpoint,
                candidateEvaluationCountSinceCheckpoint,
                predicateAcceptedCountSinceCheckpoint,
                predicateRejectedCountSinceCheckpoint,
                rejectionCountsSinceCheckpoint.toString(),
                rangeTracker.checkpointSnapshot(),
                notStoredStateHash,
                lastSuccessfulBoundary,
                firstExplicitFailureBoundary,
                firstUnobservedBoundary
        );
        branchCountsSinceCheckpoint.clear();
        branchTransitionCountsSinceCheckpoint.clear();
        rejectionCountsSinceCheckpoint.clear();
        childReplacementCountSinceCheckpoint = 0;
        transferDecisionCountSinceCheckpoint = 0;
        candidateEvaluationCountSinceCheckpoint = 0;
        predicateAcceptedCountSinceCheckpoint = 0;
        predicateRejectedCountSinceCheckpoint = 0;
        lastCheckpointTick = gameTick;
        lastCheckpointSemanticVersion = semanticVersion;
        return Optional.of(checkpoint);
    }

    public synchronized void acknowledgeCheckpointEmission(long emittedCheckpointSequence) {
        if (emittedCheckpointSequence == checkpointSequence) {
            rangeTracker.resetCheckpoint();
        }
    }

    public synchronized StoreContainerParentDecision currentParentDecision() {
        return currentParentDecision;
    }

    public synchronized long branchEpoch() {
        return branchEpoch;
    }

    public synchronized String currentBranch() {
        return currentParentDecision.selectedBranch();
    }

    public synchronized String branchCounts() {
        return branchCounts.toString();
    }

    public synchronized String branchTransitionCounts() {
        return branchTransitionCounts.toString();
    }

    public synchronized BlockPos currentFilteredCandidate() {
        return currentFilteredCandidate;
    }

    public synchronized BlockPos currentPursuit() {
        return currentPursuit;
    }

    public synchronized String currentPursuitAction() {
        return currentPursuitAction;
    }

    public synchronized int childReplacementCount() {
        return childReplacementCount;
    }

    public synchronized int rootRouteChildReplacementCount() {
        return rootRouteChildReplacementCount;
    }

    public synchronized long childLifecycleSequence() {
        return childLifecycleSequence;
    }

    public synchronized String currentRouteChildIdentity() {
        return identity(currentRouteChild);
    }

    public synchronized String currentRouteChildClass() {
        return currentRouteChild == null ? "none" : currentRouteChild.getClass().getName();
    }

    public synchronized long activeStoreAttemptSequence() {
        return activeStoreAttemptSequence;
    }

    public synchronized long activeStoreAttemptCandidateDecisionSequence() {
        return activeStoreAttemptCandidateDecisionSequence;
    }

    public synchronized long activeStoreAttemptBranchEpoch() {
        return activeStoreAttemptBranchEpoch;
    }

    public synchronized String activeStoreAttemptBranch() {
        return activeStoreAttemptBranch;
    }

    public synchronized long activeStoreAttemptRouteChildLifecycleSequence() {
        return activeStoreAttemptRouteChildLifecycleSequence;
    }

    public synchronized int activeStoreAttemptRouteChildReplacementCount() {
        return activeStoreAttemptRouteChildReplacementCount;
    }

    public synchronized StoreContainerRangeTransition currentRangeTransition() {
        return rangeTracker.currentTransition();
    }

    public synchronized StoreContainerRangeAggregate rangeSummary() {
        return rangeTracker.summary();
    }

    public synchronized int transferDecisionCount() {
        return transferDecisionCount;
    }

    public synchronized int candidateEvaluationCount() {
        return candidateEvaluationCount;
    }

    public synchronized int predicateAcceptedCount() {
        return predicateAcceptedCount;
    }

    public synchronized int predicateRejectedCount() {
        return predicateRejectedCount;
    }

    public synchronized String rejectionCounts() {
        return rejectionCounts.toString();
    }

    public synchronized int checkpointCount() {
        return checkpointCount;
    }

    public synchronized String lastSuccessfulBoundary() {
        return lastSuccessfulBoundary;
    }

    public synchronized String firstExplicitFailureBoundary() {
        return firstExplicitFailureBoundary;
    }

    public synchronized String firstUnobservedBoundary() {
        return firstUnobservedBoundary;
    }

    public static String relation(BlockPos raw, BlockPos filtered) {
        if (raw == null && filtered == null) {
            return "BOTH_NONE";
        }
        if (raw == null) {
            return "RAW_NONE_FILTERED_PRESENT";
        }
        if (filtered == null) {
            return "RAW_PRESENT_FILTERED_NONE";
        }
        return raw.equals(filtered) ? "SAME_POSITION" : "DIFFERENT_POSITION";
    }

    private void addRejections(StoreContainerCandidateObservation observation) {
        addRejection(StoreContainerCandidateRejectionReason.CHEST_ABOVE_BLOCKED_UNBREAKABLE,
                observation.rejectBlockedAboveUnbreakableCount());
        addRejection(StoreContainerCandidateRejectionReason.CONTAINER_CACHE_FULL,
                observation.rejectContainerCacheFullCount());
        addRejection(StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST,
                observation.rejectCachedDungeonCount());
        addRejection(StoreContainerCandidateRejectionReason.SPAWNER_NEAR_CHEST,
                observation.rejectSpawnerNearbyCount());
        addRejection(StoreContainerCandidateRejectionReason.UNKNOWN,
                observation.rejectUnknownCount());
    }

    private void addRejection(StoreContainerCandidateRejectionReason reason, int amount) {
        if (amount <= 0) {
            return;
        }
        rejectionCounts.put(reason, rejectionCounts.getOrDefault(reason, 0) + amount);
        rejectionCountsSinceCheckpoint.put(
                reason,
                rejectionCountsSinceCheckpoint.getOrDefault(reason, 0) + amount
        );
    }

    private static void increment(Map<String, Integer> counts, String key, int amount) {
        counts.put(key, counts.getOrDefault(key, 0) + amount);
    }

    private static String position(BlockPos position) {
        return position == null ? "none" : position.toShortString();
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String identity(Object value) {
        return value == null ? "none" : Integer.toHexString(System.identityHashCode(value));
    }
}
