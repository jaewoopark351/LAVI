package lavi.minecraft.diagnostics.mining.baritone.executor;

import java.util.concurrent.TimeUnit;

//20260830_kpopmodder: Keep unchanged executor heartbeat cadence separate from progress classification.
final class BaritoneExecutorSamplingPolicy {
    static final long UNCHANGED_SUMMARY_TICKS = 200;
    static final long UNCHANGED_SUMMARY_NANOS = TimeUnit.SECONDS.toNanos(10);

    private BaritoneExecutorSamplingPolicy() {
    }

    static boolean heartbeatDue(long ticksSinceAnyProgress,
                                long currentTick,
                                long lastHeartbeatTick,
                                long currentNanos,
                                long lastHeartbeatNanos,
                                long lastSemanticProgressNanos) {
        boolean noProgressLongEnoughByTick = ticksSinceAnyProgress >= UNCHANGED_SUMMARY_TICKS;
        boolean noProgressLongEnoughByTime = lastSemanticProgressNanos >= 0
                && currentNanos - lastSemanticProgressNanos >= UNCHANGED_SUMMARY_NANOS;
        boolean cadenceDueByTick = lastHeartbeatTick < 0
                || currentTick - lastHeartbeatTick >= UNCHANGED_SUMMARY_TICKS;
        boolean cadenceDueByTime = lastHeartbeatNanos < 0
                || currentNanos - lastHeartbeatNanos >= UNCHANGED_SUMMARY_NANOS;
        return (noProgressLongEnoughByTick || noProgressLongEnoughByTime)
                && (cadenceDueByTick || cadenceDueByTime);
    }
}
