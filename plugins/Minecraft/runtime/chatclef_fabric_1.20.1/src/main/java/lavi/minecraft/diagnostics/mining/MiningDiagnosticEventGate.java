package lavi.minecraft.diagnostics.mining;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.HashMap;
import java.util.Map;

//20260806_kpopmodder: Bound mining/path diagnostic events without changing task, pathing, retry, or timeout behavior.
final class MiningDiagnosticEventGate {
    private static final int SUMMARY_INTERVAL_TICKS = 200;
    private static final int DETAIL_LIMIT_PER_BUCKET = 256;
    private static final int SESSION_HARD_CAP = 5000;
    private static final Map<String, State> STATES = new HashMap<>();
    private static int sessionEmissions;

    private MiningDiagnosticEventGate() {
    }

    static synchronized Decision evaluate(String bucket, String fingerprint) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        String normalizedBucket = normalize(bucket);
        String normalizedFingerprint = normalize(fingerprint);
        State state = STATES.computeIfAbsent(normalizedBucket, ignored -> new State(tick));

        if (!normalizedFingerprint.equals(state.fingerprint)) {
            if (!canEmitDetail(state)) {
                state.suppressedCount++;
                state.lastObservedTick = tick;
                return Decision.suppressed(state);
            }
            int suppressed = state.suppressedCount;
            long firstObserved = state.firstObservedTick;
            state.fingerprint = normalizedFingerprint;
            state.suppressedCount = 0;
            state.firstObservedTick = tick;
            state.lastObservedTick = tick;
            state.detailEmissions++;
            sessionEmissions++;
            return Decision.emit(false, suppressed, firstObserved, tick);
        }

        state.suppressedCount++;
        state.lastObservedTick = tick;
        if (tick - state.lastSummaryTick >= SUMMARY_INTERVAL_TICKS && canEmitDetail(state)) {
            int suppressed = state.suppressedCount;
            long firstObserved = state.firstObservedTick;
            state.suppressedCount = 0;
            state.firstObservedTick = tick;
            state.lastSummaryTick = tick;
            state.detailEmissions++;
            sessionEmissions++;
            return Decision.emit(true, suppressed, firstObserved, tick);
        }
        return Decision.suppressed(state);
    }

    private static boolean canEmitDetail(State state) {
        return sessionEmissions < SESSION_HARD_CAP && state.detailEmissions < DETAIL_LIMIT_PER_BUCKET;
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        return value.length() <= 360 ? value : value.substring(0, 360) + "...";
    }

    static final class Decision {
        final boolean emit;
        final boolean summary;
        final int suppressedCount;
        final long firstObservedTick;
        final long lastObservedTick;

        private Decision(boolean emit, boolean summary, int suppressedCount, long firstObservedTick, long lastObservedTick) {
            this.emit = emit;
            this.summary = summary;
            this.suppressedCount = suppressedCount;
            this.firstObservedTick = firstObservedTick;
            this.lastObservedTick = lastObservedTick;
        }

        static Decision emit(boolean summary, int suppressedCount, long firstObservedTick, long lastObservedTick) {
            return new Decision(true, summary, suppressedCount, firstObservedTick, lastObservedTick);
        }

        static Decision suppressed(State state) {
            return new Decision(false, false, state.suppressedCount, state.firstObservedTick, state.lastObservedTick);
        }
    }

    private static final class State {
        String fingerprint = "";
        int suppressedCount;
        int detailEmissions;
        long firstObservedTick;
        long lastObservedTick;
        long lastSummaryTick;

        State(long tick) {
            firstObservedTick = tick;
            lastObservedTick = tick;
            lastSummaryTick = tick;
        }
    }
}
