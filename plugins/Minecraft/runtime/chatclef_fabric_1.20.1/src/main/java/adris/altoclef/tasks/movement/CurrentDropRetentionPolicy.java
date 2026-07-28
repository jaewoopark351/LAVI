package adris.altoclef.tasks.movement;

import adris.altoclef.util.helpers.WorldHelper;

//20260728_kpopmodder: Added this type file to isolate current dropped-item retry and minimum-lock timing state.
final class CurrentDropRetentionPolicy {
    private final int retryGraceTicks;
    private final int minLockTicks;
    private int lockStartTick = -1;
    private int failureStartTick = -1;
    private int failureLastTick = -1;
    private int failureCount = 0;

    CurrentDropRetentionPolicy(int retryGraceTicks, int minLockTicks) {
        this.retryGraceTicks = retryGraceTicks;
        this.minLockTicks = minLockTicks;
    }

    boolean recordFailureAndIsWithinGrace() {
        int now = WorldHelper.getTicks();
        if (failureStartTick < 0) {
            failureStartTick = now;
            failureLastTick = now;
            failureCount = 1;
            return true;
        }
        if (failureLastTick != now) {
            failureLastTick = now;
            failureCount++;
        }
        return now - failureStartTick < retryGraceTicks;
    }

    boolean hasFailure() {
        return failureStartTick >= 0;
    }

    int retryGraceTicks() {
        return retryGraceTicks;
    }

    int retryTicksRemaining() {
        if (failureStartTick < 0) {
            return retryGraceTicks;
        }
        return Math.max(0, retryGraceTicks - (WorldHelper.getTicks() - failureStartTick));
    }

    int failureCount() {
        return failureCount;
    }

    void resetFailure() {
        failureStartTick = -1;
        failureLastTick = -1;
        failureCount = 0;
    }

    void armLock() {
        lockStartTick = WorldHelper.getTicks();
    }

    boolean isWithinMinLock() {
        return lockStartTick >= 0
                && WorldHelper.getTicks() - lockStartTick < minLockTicks;
    }

    int minLockTicksRemaining() {
        if (lockStartTick < 0) {
            return 0;
        }
        return Math.max(0, minLockTicks - (WorldHelper.getTicks() - lockStartTick));
    }

    void resetLock() {
        lockStartTick = -1;
    }
}
