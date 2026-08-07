package lavi.minecraft.diagnostics.container.store;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Rate-limit StoreInAnyContainerTask progress diagnostics during post-completion loops.
final class StoreInAnyContainerEmissionLimiter {
    static final int SESSION_HARD_CAP = 512;
    static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private int emittedCount;
    private boolean capLogged;
    private final Map<String, RepeatState> repeatStates = new HashMap<>();

    synchronized StoreInAnyContainerEmissionDecision evaluate(String key, long clientTickId) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return StoreInAnyContainerEmissionDecision.suppress();
            }
            capLogged = true;
            return StoreInAnyContainerEmissionDecision.cap();
        }

        RepeatState state = repeatStates.computeIfAbsent(key, ignored -> new RepeatState());
        if (!state.seen) {
            state.seen = true;
            state.lastRepeatSummaryTick = clientTickId;
            emittedCount++;
            return StoreInAnyContainerEmissionDecision.event(0);
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
            return StoreInAnyContainerEmissionDecision.summary(suppressedBefore);
        }
        return StoreInAnyContainerEmissionDecision.suppress();
    }

    private static final class RepeatState {
        private boolean seen;
        private int suppressedRepeats;
        private long lastRepeatSummaryTick = -1;
    }
}
