package lavi.minecraft.diagnostics.inventory;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Bound repeated InventorySubTracker diagnostics without hiding boundary changes.
public final class InventoryScanEventLimiter {
    public static final int SESSION_HARD_CAP = 5000;

    private static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200;

    private int emittedCount;
    private boolean capLogged;
    private final Map<String, RepeatState> repeatStates = new HashMap<>();

    public synchronized InventoryScanEmissionDecision evaluate(String key, long clientTickId, boolean critical) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (capLogged) {
                return InventoryScanEmissionDecision.suppress();
            }
            capLogged = true;
            return InventoryScanEmissionDecision.cap();
        }

        if (critical) {
            emittedCount++;
            return InventoryScanEmissionDecision.event(0);
        }

        RepeatState state = repeatStates.computeIfAbsent(key, ignored -> new RepeatState());
        if (!state.seen) {
            state.seen = true;
            state.lastRepeatSummaryTick = clientTickId;
            emittedCount++;
            return InventoryScanEmissionDecision.event(0);
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
            return InventoryScanEmissionDecision.summary(suppressedBefore);
        }
        return InventoryScanEmissionDecision.suppress();
    }

    private static final class RepeatState {
        private boolean seen;
        private int suppressedRepeats;
        private long lastRepeatSummaryTick = -1;
    }
}
