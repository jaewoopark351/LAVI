package adris.altoclef.tasks.resources.mining;

import adris.altoclef.tasksystem.Task;

//20260728_kpopmodder: Added this diagnostics holder so MineOrCollectTask stays focused on task flow.
final class MineOrCollectDiagnostics {
    private int blockPreferredCount = 0;
    private int dropPreferredCount = 0;
    private int pickupGracePreferredCount = 0;
    private int pickupContinuationPreferredCount = 0;
    private int interactionPausedDropPreferredCount = 0;
    private int miningTargetSwitchCount = 0;
    private int miningTargetRetainCount = 0;
    private int miningTargetReleaseCount = 0;
    private int pickupTargetSwitchCount = 0;
    private int temporaryMiningSkipCount = 0;
    private int postMiningSweepPreferredCount = 0;
    private int postMiningSweepWaitCount = 0;
    private int postMiningSweepFinishDelayCount = 0;
    private int localMiningPreferredCount = 0;
    private String lastSelectedGoalKey = "";

    void recordBlockPreferred() {
        blockPreferredCount++;
    }

    void recordDropPreferred() {
        dropPreferredCount++;
    }

    void recordPickupGracePreferred() {
        pickupGracePreferredCount++;
    }

    void recordPickupContinuationPreferred() {
        pickupContinuationPreferredCount++;
    }

    void recordInteractionPausedDropPreferred() {
        interactionPausedDropPreferredCount++;
    }

    void recordMiningTargetRetained() {
        miningTargetRetainCount++;
    }

    void recordMiningTargetRelease() {
        miningTargetReleaseCount++;
    }

    void recordTemporaryMiningSkip() {
        temporaryMiningSkipCount++;
    }

    void recordPostMiningSweepPreferred() {
        postMiningSweepPreferredCount++;
    }

    void recordPostMiningSweepWait() {
        postMiningSweepWaitCount++;
    }

    void recordPostMiningSweepFinishDelay() {
        postMiningSweepFinishDelayCount++;
    }

    void recordLocalMiningPreferred() {
        localMiningPreferredCount++;
    }

    void recordGoalSelection(String goalKey, boolean miningGoal) {
        if (goalKey.equals(lastSelectedGoalKey)) {
            return;
        }
        lastSelectedGoalKey = goalKey;
        if (miningGoal) {
            miningTargetSwitchCount++;
        } else {
            pickupTargetSwitchCount++;
        }
    }

    boolean hasEvents() {
        return blockPreferredCount
                + dropPreferredCount
                + pickupGracePreferredCount
                + pickupContinuationPreferredCount
                + interactionPausedDropPreferredCount
                + miningTargetSwitchCount
                + miningTargetRetainCount
                + miningTargetReleaseCount
                + pickupTargetSwitchCount
                + temporaryMiningSkipCount
                + postMiningSweepPreferredCount
                + postMiningSweepWaitCount
                + postMiningSweepFinishDelayCount
                + localMiningPreferredCount > 0;
    }

    String summary(Task interruptTask) {
        return "choice summary: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                + ", blockPreferredTicks=" + blockPreferredCount
                + ", dropPreferredTicks=" + dropPreferredCount
                + ", pickupGracePreferredTicks=" + pickupGracePreferredCount
                + ", pickupContinuationPreferredTicks=" + pickupContinuationPreferredCount
                + ", interactionPausedDropPreferredTicks=" + interactionPausedDropPreferredCount
                + ", miningTargetSwitches=" + miningTargetSwitchCount
                + ", miningTargetRetainedTicks=" + miningTargetRetainCount
                + ", miningTargetReleases=" + miningTargetReleaseCount
                + ", pickupTargetSwitches=" + pickupTargetSwitchCount
                + ", temporaryMiningSkips=" + temporaryMiningSkipCount
                + ", postMiningSweepPreferredTicks=" + postMiningSweepPreferredCount
                + ", postMiningSweepWaitTicks=" + postMiningSweepWaitCount
                + ", postMiningSweepFinishDelays=" + postMiningSweepFinishDelayCount
                + ", localMiningPreferredTicks=" + localMiningPreferredCount
                + ", lastGoal=" + lastSelectedGoalKey;
    }

    void reset() {
        blockPreferredCount = 0;
        dropPreferredCount = 0;
        pickupGracePreferredCount = 0;
        pickupContinuationPreferredCount = 0;
        interactionPausedDropPreferredCount = 0;
        miningTargetSwitchCount = 0;
        miningTargetRetainCount = 0;
        miningTargetReleaseCount = 0;
        pickupTargetSwitchCount = 0;
        temporaryMiningSkipCount = 0;
        postMiningSweepPreferredCount = 0;
        postMiningSweepWaitCount = 0;
        postMiningSweepFinishDelayCount = 0;
        localMiningPreferredCount = 0;
        lastSelectedGoalKey = "";
    }
}
