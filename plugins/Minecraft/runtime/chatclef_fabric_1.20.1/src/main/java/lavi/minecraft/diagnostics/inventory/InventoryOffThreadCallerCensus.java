package lavi.minecraft.diagnostics.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//20260805_kpopmodder: Summarize off-client inventory scan callers without changing thread behavior.
public final class InventoryOffThreadCallerCensus {
    private static final long SUMMARY_INTERVAL_TICKS = 200;
    private static final long SUMMARY_INTERVAL_NANOS = 10_000_000_000L;

    private final ConcurrentMap<String, CallerState> callers = new ConcurrentHashMap<>();

    public InventoryOffThreadCallerSummary observe(InventoryScanContext context,
                                                   int activeScanCountNow,
                                                   long clientTickId,
                                                   long nowNanos) {
        if (context == null || context.clientThread()) {
            return null;
        }
        String fingerprint = fingerprint(context);
        CallerState state = callers.computeIfAbsent(fingerprint, ignored -> new CallerState(clientTickId, nowNanos));
        synchronized (state) {
            state.observationCount++;
            state.lastObservedClientTickId = clientTickId;
            state.lastObservedNanos = nowNanos;
            state.maxActiveScanCount = Math.max(state.maxActiveScanCount, activeScanCountNow);
            if (activeScanCountNow > 1) {
                state.overlapObservationCount++;
            }
            if (state.shouldEmit(clientTickId, nowNanos)) {
                state.lastEmittedClientTickId = clientTickId;
                state.lastEmittedNanos = nowNanos;
                return new InventoryOffThreadCallerSummary(
                        fingerprint,
                        state.firstObservedClientTickId,
                        state.lastObservedClientTickId,
                        state.observationCount,
                        state.overlapObservationCount,
                        state.maxActiveScanCount
                );
            }
            return null;
        }
    }

    private static String fingerprint(InventoryScanContext context) {
        String raw = context.threadName()
                + "|"
                + context.caller().callerBoundary()
                + "|"
                + context.caller().callerTopFrames();
        return Integer.toHexString(raw.hashCode());
    }

    public static final class InventoryOffThreadCallerSummary {
        private final String callerFingerprint;
        private final long firstObservedClientTickId;
        private final long lastObservedClientTickId;
        private final int observationCount;
        private final int overlapObservationCount;
        private final int maxActiveScanCount;

        private InventoryOffThreadCallerSummary(String callerFingerprint,
                                                long firstObservedClientTickId,
                                                long lastObservedClientTickId,
                                                int observationCount,
                                                int overlapObservationCount,
                                                int maxActiveScanCount) {
            this.callerFingerprint = callerFingerprint;
            this.firstObservedClientTickId = firstObservedClientTickId;
            this.lastObservedClientTickId = lastObservedClientTickId;
            this.observationCount = observationCount;
            this.overlapObservationCount = overlapObservationCount;
            this.maxActiveScanCount = maxActiveScanCount;
        }

        public String callerFingerprint() {
            return callerFingerprint;
        }

        public long firstObservedClientTickId() {
            return firstObservedClientTickId;
        }

        public long lastObservedClientTickId() {
            return lastObservedClientTickId;
        }

        public int observationCount() {
            return observationCount;
        }

        public int overlapObservationCount() {
            return overlapObservationCount;
        }

        public int maxActiveScanCount() {
            return maxActiveScanCount;
        }
    }

    private static final class CallerState {
        private final long firstObservedClientTickId;
        private final long firstObservedNanos;
        private long lastObservedClientTickId;
        private long lastObservedNanos;
        private long lastEmittedClientTickId = Long.MIN_VALUE;
        private long lastEmittedNanos = Long.MIN_VALUE;
        private int observationCount;
        private int overlapObservationCount;
        private int maxActiveScanCount;

        private CallerState(long firstObservedClientTickId, long firstObservedNanos) {
            this.firstObservedClientTickId = firstObservedClientTickId;
            this.firstObservedNanos = firstObservedNanos;
        }

        private boolean shouldEmit(long clientTickId, long nowNanos) {
            return observationCount == 1
                    || clientTickId - lastEmittedClientTickId >= SUMMARY_INTERVAL_TICKS
                    || nowNanos - lastEmittedNanos >= SUMMARY_INTERVAL_NANOS;
        }
    }
}
