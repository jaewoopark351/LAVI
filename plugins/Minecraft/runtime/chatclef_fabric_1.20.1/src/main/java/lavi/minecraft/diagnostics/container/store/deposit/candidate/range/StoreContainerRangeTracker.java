package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import java.util.Objects;

public final class StoreContainerRangeTracker {
    private StoreContainerRangeSnapshot previous = StoreContainerRangeSnapshot.unavailable();
    private StoreContainerRangeTransition currentTransition = StoreContainerRangeTransition.unavailable();
    private int raw50CrossingCount;
    private int currentTry70CrossingCount;
    private int branchChangeAtRangeCrossingCount;
    private int resourceChildInterruptedAtRangeCrossingCount;
    private int raw50CrossingCountSinceCheckpoint;
    private int currentTry70CrossingCountSinceCheckpoint;
    private int branchChangeAtRangeCrossingCountSinceCheckpoint;
    private int resourceChildInterruptedAtRangeCrossingCountSinceCheckpoint;
    private String firstRangeCrossingSample = "none";
    private String lastRangeCrossingSample = "none";
    private String firstRangeCrossingSampleSinceCheckpoint = "none";
    private String lastRangeCrossingSampleSinceCheckpoint = "none";
    private long lastResourceInterruptionDecisionSequence = -1;

    public StoreContainerRangeTransition observe(StoreContainerRangeSnapshot current,
                                                 String selectedBranch,
                                                 long branchEpoch,
                                                 boolean branchChanged) {
        StoreContainerRangeSnapshot resolved = current == null
                ? StoreContainerRangeSnapshot.unavailable()
                : current;
        boolean rawPreviousAvailable = comparableRaw(previous, resolved);
        boolean currentPreviousAvailable = comparableCurrentTry(previous, resolved);
        String rawCrossing = crossing(
                rawPreviousAvailable,
                previous.rawWithin50(),
                resolved.rawWithin50()
        );
        String currentCrossing = crossing(
                currentPreviousAvailable,
                previous.currentTryWithin70(),
                resolved.currentTryWithin70()
        );
        currentTransition = new StoreContainerRangeTransition(
                resolved,
                rawPreviousAvailable,
                previous.rawWithin50(),
                rawCrossing,
                currentPreviousAvailable,
                previous.currentTryWithin70(),
                currentCrossing,
                selectedBranch,
                branchEpoch,
                branchChanged
        );
        recordCrossing(currentTransition);
        previous = resolved;
        return currentTransition;
    }

    public boolean recordResourceInterruption(long candidateDecisionSequence) {
        if (!currentTransition.hasCrossing()
                || candidateDecisionSequence == lastResourceInterruptionDecisionSequence) {
            return false;
        }
        lastResourceInterruptionDecisionSequence = candidateDecisionSequence;
        resourceChildInterruptedAtRangeCrossingCount++;
        resourceChildInterruptedAtRangeCrossingCountSinceCheckpoint++;
        return true;
    }

    public StoreContainerRangeTransition currentTransition() {
        return currentTransition;
    }

    public StoreContainerRangeAggregate checkpointSnapshot() {
        return new StoreContainerRangeAggregate(
                raw50CrossingCountSinceCheckpoint,
                currentTry70CrossingCountSinceCheckpoint,
                branchChangeAtRangeCrossingCountSinceCheckpoint,
                resourceChildInterruptedAtRangeCrossingCountSinceCheckpoint,
                firstRangeCrossingSampleSinceCheckpoint,
                lastRangeCrossingSampleSinceCheckpoint
        );
    }

    public void resetCheckpoint() {
        raw50CrossingCountSinceCheckpoint = 0;
        currentTry70CrossingCountSinceCheckpoint = 0;
        branchChangeAtRangeCrossingCountSinceCheckpoint = 0;
        resourceChildInterruptedAtRangeCrossingCountSinceCheckpoint = 0;
        firstRangeCrossingSampleSinceCheckpoint = "none";
        lastRangeCrossingSampleSinceCheckpoint = "none";
    }

    public StoreContainerRangeAggregate summary() {
        return new StoreContainerRangeAggregate(
                raw50CrossingCount,
                currentTry70CrossingCount,
                branchChangeAtRangeCrossingCount,
                resourceChildInterruptedAtRangeCrossingCount,
                firstRangeCrossingSample,
                lastRangeCrossingSample
        );
    }

    private void recordCrossing(StoreContainerRangeTransition transition) {
        if (!transition.hasCrossing()) {
            return;
        }
        if (!"NONE".equals(transition.rawRangeCrossing())) {
            raw50CrossingCount++;
            raw50CrossingCountSinceCheckpoint++;
        }
        if (!"NONE".equals(transition.currentTryRangeCrossing())) {
            currentTry70CrossingCount++;
            currentTry70CrossingCountSinceCheckpoint++;
        }
        if (transition.branchChanged()) {
            branchChangeAtRangeCrossingCount++;
            branchChangeAtRangeCrossingCountSinceCheckpoint++;
        }
        String sample = transition.sample();
        if ("none".equals(firstRangeCrossingSample)) {
            firstRangeCrossingSample = sample;
        }
        lastRangeCrossingSample = sample;
        if ("none".equals(firstRangeCrossingSampleSinceCheckpoint)) {
            firstRangeCrossingSampleSinceCheckpoint = sample;
        }
        lastRangeCrossingSampleSinceCheckpoint = sample;
    }

    private static boolean comparableRaw(StoreContainerRangeSnapshot before,
                                         StoreContainerRangeSnapshot after) {
        return before.rawRangeEvaluated()
                && after.rawRangeEvaluated()
                && Objects.equals(before.rawTarget(), after.rawTarget());
    }

    private static boolean comparableCurrentTry(StoreContainerRangeSnapshot before,
                                                StoreContainerRangeSnapshot after) {
        return before.currentTryRangeEvaluated()
                && after.currentTryRangeEvaluated()
                && Objects.equals(before.currentTryTarget(), after.currentTryTarget());
    }

    private static String crossing(boolean previousAvailable, boolean before, boolean after) {
        if (!previousAvailable || before == after) {
            return "NONE";
        }
        return before ? "INSIDE_TO_OUTSIDE" : "OUTSIDE_TO_INSIDE";
    }
}
