package adris.altoclef.tasks.movement.pickup;

//20260728_kpopmodder: Added this type file to keep pickup stability counters out of PickupDroppedItemTask control flow.
final class PickupDropDiagnostics {
    int candidateCount = 0;
    int lockCount = 0;
    int switchCount = 0;
    int retainCount = 0;
    int minLockRetainCount = 0;
    int movementStallCount = 0;
    int retryStartCount = 0;
    int retryContinueCount = 0;
    int retryRecoveredCount = 0;
    int retryExpiredCount = 0;
    //20260728_kpopmodder: Separate already-collected drops from real abandon failures in latest.log.
    int consumedOrRemovedCount = 0;
    int abandonCount = 0;
    int blacklistCount = 0;

    boolean hasEvents() {
        return candidateCount
                + lockCount
                + switchCount
                + retainCount
                + minLockRetainCount
                + movementStallCount
                + retryStartCount
                + retryContinueCount
                + retryRecoveredCount
                + retryExpiredCount
                + consumedOrRemovedCount
                + abandonCount
                + blacklistCount > 0;
    }

    void reset() {
        candidateCount = 0;
        lockCount = 0;
        switchCount = 0;
        retainCount = 0;
        minLockRetainCount = 0;
        movementStallCount = 0;
        retryStartCount = 0;
        retryContinueCount = 0;
        retryRecoveredCount = 0;
        retryExpiredCount = 0;
        consumedOrRemovedCount = 0;
        abandonCount = 0;
        blacklistCount = 0;
    }

    String describeStopSummary(String interruptedBy, String currentDropDescription) {
        return "stop summary: interruptedBy=" + interruptedBy
                + ", candidates=" + candidateCount
                + ", locks=" + lockCount
                + ", switches=" + switchCount
                + ", retained=" + retainCount
                + ", minLockRetained=" + minLockRetainCount
                + ", movementStalls=" + movementStallCount
                + ", retryStarts=" + retryStartCount
                + ", retryTicks=" + retryContinueCount
                + ", retryRecoveries=" + retryRecoveredCount
                + ", retryExpired=" + retryExpiredCount
                + ", consumedOrRemoved=" + consumedOrRemovedCount
                + ", abandons=" + abandonCount
                + ", blacklisted=" + blacklistCount
                + ", currentDrop=" + currentDropDescription;
    }
}
