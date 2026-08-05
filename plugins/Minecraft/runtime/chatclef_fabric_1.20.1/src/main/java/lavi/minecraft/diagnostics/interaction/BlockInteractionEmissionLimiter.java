package lavi.minecraft.diagnostics.interaction;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Bound repeated container interaction diagnostics without hiding state changes.
public final class BlockInteractionEmissionLimiter {
    private static final int SESSION_HARD_CAP = 5000;
    private static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private int emittedCount;
    private boolean capLogged;
    private final Map<String, RepeatState> repeatStates = new HashMap<>();

    public synchronized BlockInteractionEmissionDecision evaluate(String key, long clientTickId) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return BlockInteractionEmissionDecision.suppress();
            }
            capLogged = true;
            return BlockInteractionEmissionDecision.cap();
        }

        RepeatState state = repeatStates.computeIfAbsent(key, ignored -> new RepeatState());
        if (!state.seen) {
            state.seen = true;
            state.lastRepeatSummaryTick = clientTickId;
            emittedCount++;
            return BlockInteractionEmissionDecision.event(0);
        }

        state.suppressedRepeats++;
        if (state.lastRepeatSummaryTick < 0) {
            state.lastRepeatSummaryTick = clientTickId;
        }
        if (clientTickId - state.lastRepeatSummaryTick >= REPEAT_SUMMARY_INTERVAL_TICKS) {
            int suppressedBefore = state.suppressedRepeats;
            state.suppressedRepeats = 0;
            state.lastRepeatSummaryTick = clientTickId;
            emittedCount++;
            return BlockInteractionEmissionDecision.summary(suppressedBefore);
        }
        return BlockInteractionEmissionDecision.suppress();
    }

    private static final class RepeatState {
        private boolean seen;
        private int suppressedRepeats;
        private long lastRepeatSummaryTick = -1;
    }
}
