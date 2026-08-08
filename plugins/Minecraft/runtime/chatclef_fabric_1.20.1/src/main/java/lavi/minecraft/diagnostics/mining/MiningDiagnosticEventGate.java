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
    private static boolean limitReportClaimed;

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
                return Decision.suppressed(state, limitDecision(state));
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
        if (!canEmitDetail(state)) {
            return Decision.suppressed(state, limitDecision(state));
        }
        if (tick - state.lastSummaryTick >= SUMMARY_INTERVAL_TICKS) {
            int suppressed = state.suppressedCount;
            long firstObserved = state.firstObservedTick;
            state.suppressedCount = 0;
            state.firstObservedTick = tick;
            state.lastSummaryTick = tick;
            state.detailEmissions++;
            sessionEmissions++;
            return Decision.emit(true, suppressed, firstObserved, tick);
        }
        return Decision.suppressed(state, LimitDecision.none());
    }

    private static boolean canEmitDetail(State state) {
        return sessionEmissions < SESSION_HARD_CAP && state.detailEmissions < DETAIL_LIMIT_PER_BUCKET;
    }

    private static LimitDecision limitDecision(State state) {
        String reason = sessionEmissions >= SESSION_HARD_CAP
                ? "SESSION_HARD_CAP"
                : "DETAIL_LIMIT_PER_BUCKET";
        boolean report = !limitReportClaimed;
        if (report) {
            limitReportClaimed = true;
        }
        return new LimitDecision(
                true,
                report,
                reason,
                sessionEmissions,
                SESSION_HARD_CAP,
                state.detailEmissions,
                DETAIL_LIMIT_PER_BUCKET
        );
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
        final boolean gateLimitReached;
        final boolean reportGateLimit;
        final String gateLimitReason;
        final int sessionEmissions;
        final int sessionHardCap;
        final int bucketDetailEmissions;
        final int bucketDetailLimit;

        private Decision(boolean emit,
                         boolean summary,
                         int suppressedCount,
                         long firstObservedTick,
                         long lastObservedTick,
                         LimitDecision limit) {
            this.emit = emit;
            this.summary = summary;
            this.suppressedCount = suppressedCount;
            this.firstObservedTick = firstObservedTick;
            this.lastObservedTick = lastObservedTick;
            this.gateLimitReached = limit.reached;
            this.reportGateLimit = limit.report;
            this.gateLimitReason = limit.reason;
            this.sessionEmissions = limit.sessionEmissions;
            this.sessionHardCap = limit.sessionHardCap;
            this.bucketDetailEmissions = limit.bucketDetailEmissions;
            this.bucketDetailLimit = limit.bucketDetailLimit;
        }

        static Decision emit(boolean summary, int suppressedCount, long firstObservedTick, long lastObservedTick) {
            return new Decision(true, summary, suppressedCount, firstObservedTick, lastObservedTick, LimitDecision.none());
        }

        static Decision suppressed(State state, LimitDecision limit) {
            return new Decision(false, false, state.suppressedCount, state.firstObservedTick, state.lastObservedTick, limit);
        }
    }

    private record LimitDecision(boolean reached,
                                 boolean report,
                                 String reason,
                                 int sessionEmissions,
                                 int sessionHardCap,
                                 int bucketDetailEmissions,
                                 int bucketDetailLimit) {
        static LimitDecision none() {
            return new LimitDecision(false, false, "none", MiningDiagnosticEventGate.sessionEmissions, SESSION_HARD_CAP, 0,
                    DETAIL_LIMIT_PER_BUCKET);
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
