package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

//20260807_kpopmodder: Bound tool-save snapshot consumption diagnostics by semantic repeat key.
final class ToolSavePolicySnapshotEmissionLimiter {
    static final int SESSION_HARD_CAP = 5000;
    static final int MAX_REPEAT_STATES = 16;

    private static final int DETAIL_LIMIT_PER_BUCKET = 256;
    private static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private final Map<String, RepeatState> repeatStates =
            new LinkedHashMap<>(MAX_REPEAT_STATES, 0.75f, true);
    private int emittedCount;
    private boolean capLogged;

    synchronized Decision evaluate(String key) {
        return evaluate(key, ChatClefDiagnostics.currentClientTickId());
    }

    synchronized Decision evaluate(String key, long observedTick) {
        long tick = Math.max(0L, observedTick);
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return Decision.suppress();
            }
            capLogged = true;
            return Decision.cap();
        }

        String normalizedKey = normalize(key);
        RepeatState state = repeatStates.get(normalizedKey);
        if (state == null) {
            evictOldestRepeatStateIfFull();
            state = new RepeatState(tick);
            repeatStates.put(normalizedKey, state);
        }
        long effectiveTick = Math.max(tick, state.lastObservedTick);
        if (!state.seen) {
            state.seen = true;
            state.detailEmissions++;
            emittedCount++;
            return Decision.event(0, state.firstObservedTick, effectiveTick);
        }

        state.suppressedRepeats = saturatingIncrement(state.suppressedRepeats);
        state.lastObservedTick = effectiveTick;
        if (effectiveTick >= state.lastSummaryTick
                && effectiveTick - state.lastSummaryTick >= REPEAT_SUMMARY_INTERVAL_TICKS
                && state.detailEmissions < DETAIL_LIMIT_PER_BUCKET) {
            int suppressedBefore = state.suppressedRepeats;
            long firstObserved = state.firstObservedTick;
            state.suppressedRepeats = 0;
            state.firstObservedTick = effectiveTick;
            state.lastSummaryTick = effectiveTick;
            state.detailEmissions++;
            emittedCount++;
            return Decision.summary(suppressedBefore, firstObserved, effectiveTick);
        }
        return Decision.suppress();
    }

    synchronized int repeatStateCount() {
        return repeatStates.size();
    }

    synchronized void clearForModeOff() {
        repeatStates.clear();
        emittedCount = 0;
        capLogged = false;
    }

    private void evictOldestRepeatStateIfFull() {
        if (repeatStates.size() < MAX_REPEAT_STATES) {
            return;
        }
        Iterator<String> iterator = repeatStates.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static int saturatingIncrement(int value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        if (value.length() <= 360) {
            return value;
        }
        String suffix = "#h=" + Integer.toUnsignedString(value.hashCode(), 16);
        int prefixLength = Math.max(0, 360 - suffix.length());
        return value.substring(0, prefixLength) + suffix;
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
