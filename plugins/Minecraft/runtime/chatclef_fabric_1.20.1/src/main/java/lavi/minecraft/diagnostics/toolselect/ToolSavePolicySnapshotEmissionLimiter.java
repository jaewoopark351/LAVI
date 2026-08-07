package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Bound tool-save snapshot consumption diagnostics by semantic repeat key.
final class ToolSavePolicySnapshotEmissionLimiter {
    static final int SESSION_HARD_CAP = 5000;

    private static final int DETAIL_LIMIT_PER_BUCKET = 256;
    private static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private final Map<String, RepeatState> repeatStates = new HashMap<>();
    private int emittedCount;
    private boolean capLogged;

    synchronized Decision evaluate(String key) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return Decision.suppress();
            }
            capLogged = true;
            return Decision.cap();
        }

        RepeatState state = repeatStates.computeIfAbsent(normalize(key), ignored -> new RepeatState(tick));
        if (!state.seen) {
            state.seen = true;
            state.detailEmissions++;
            emittedCount++;
            return Decision.event(0, state.firstObservedTick, tick);
        }

        state.suppressedRepeats++;
        state.lastObservedTick = tick;
        if (tick - state.lastSummaryTick >= REPEAT_SUMMARY_INTERVAL_TICKS && state.detailEmissions < DETAIL_LIMIT_PER_BUCKET) {
            int suppressedBefore = state.suppressedRepeats;
            long firstObserved = state.firstObservedTick;
            state.suppressedRepeats = 0;
            state.firstObservedTick = tick;
            state.lastSummaryTick = tick;
            state.detailEmissions++;
            emittedCount++;
            return Decision.summary(suppressedBefore, firstObserved, tick);
        }
        return Decision.suppress();
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        return value.length() <= 360 ? value : value.substring(0, 360) + "...";
    }

    static final class Decision {
        private static final Decision SUPPRESS = new Decision(false, false, false, 0, -1, -1);

        final boolean emitEvent;
        final boolean emitSummary;
        final boolean emitCap;
        final int suppressedRepeatCount;
        final long firstObservedTick;
        final long lastObservedTick;

        private Decision(boolean emitEvent,
                         boolean emitSummary,
                         boolean emitCap,
                         int suppressedRepeatCount,
                         long firstObservedTick,
                         long lastObservedTick) {
            this.emitEvent = emitEvent;
            this.emitSummary = emitSummary;
            this.emitCap = emitCap;
            this.suppressedRepeatCount = suppressedRepeatCount;
            this.firstObservedTick = firstObservedTick;
            this.lastObservedTick = lastObservedTick;
        }

        static Decision event(int suppressedRepeatCount, long firstObservedTick, long lastObservedTick) {
            return new Decision(true, false, false, suppressedRepeatCount, firstObservedTick, lastObservedTick);
        }

        static Decision summary(int suppressedRepeatCount, long firstObservedTick, long lastObservedTick) {
            return new Decision(false, true, false, suppressedRepeatCount, firstObservedTick, lastObservedTick);
        }

        static Decision cap() {
            return new Decision(false, false, true, 0, -1, -1);
        }

        static Decision suppress() {
            return SUPPRESS;
        }
    }

    private static final class RepeatState {
        boolean seen;
        int suppressedRepeats;
        int detailEmissions;
        long firstObservedTick;
        long lastObservedTick;
        long lastSummaryTick;

        RepeatState(long tick) {
            firstObservedTick = tick;
            lastObservedTick = tick;
            lastSummaryTick = tick;
        }
    }
}
