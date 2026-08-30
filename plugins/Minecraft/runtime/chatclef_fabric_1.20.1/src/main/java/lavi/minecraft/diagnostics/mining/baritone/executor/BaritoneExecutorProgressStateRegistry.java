package lavi.minecraft.diagnostics.mining.baritone.executor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//20260830_kpopmodder: Own only bounded executor-progress state lookup.
final class BaritoneExecutorProgressStateRegistry {
    private static final int STATE_HARD_CAP = 256;
    private static final ConcurrentMap<String, BaritoneExecutorProgressState> STATES = new ConcurrentHashMap<>();

    private BaritoneExecutorProgressStateRegistry() {
    }

    static BaritoneExecutorProgressState stateFor(BaritoneExecutorProgressSnapshot snapshot) {
        if (STATES.size() > STATE_HARD_CAP) {
            STATES.clear();
        }
        return STATES.computeIfAbsent(snapshot.pathingBehaviorIdentity(),
                ignored -> new BaritoneExecutorProgressState());
    }
}
