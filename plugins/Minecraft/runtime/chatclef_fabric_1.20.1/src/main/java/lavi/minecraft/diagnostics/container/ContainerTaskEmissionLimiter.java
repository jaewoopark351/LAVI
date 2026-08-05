package lavi.minecraft.diagnostics.container;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Rate-limit repeated container acquisition loop observations.
final class ContainerTaskEmissionLimiter {
    static final int SESSION_HARD_CAP = 5000;

    private static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private int emittedCount;
    private boolean capLogged;
    private final Map<String, RepeatState> repeatStates = new HashMap<>();

    synchronized ContainerTaskEmissionDecision evaluate(String key, long clientTickId) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return ContainerTaskEmissionDecision.suppress();
            }
            capLogged = true;
            return ContainerTaskEmissionDecision.cap();
        }

        RepeatState state = repeatStates.computeIfAbsent(key, ignored -> new RepeatState());
        if (!state.seen) {
            state.seen = true;
            state.lastRepeatSummaryTick = clientTickId;
            emittedCount++;
            return ContainerTaskEmissionDecision.event(0);
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
            return ContainerTaskEmissionDecision.summary(suppressedBefore);
        }
        return ContainerTaskEmissionDecision.suppress();
    }

    private static final class RepeatState {
        private boolean seen;
        private int suppressedRepeats;
        private long lastRepeatSummaryTick = -1;
    }
}
