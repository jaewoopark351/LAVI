package lavi.minecraft.diagnostics.mining.gate;

import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticSessionBudget;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//20260830_kpopmodder: Own bounded mining gate state retention and diagnostic-session reset.
public final class MiningDiagnosticGateStateStore {
    private final int maximumTrackedStates;
    private final Map<String, MiningDiagnosticGateState> states = new HashMap<>();
    private final Set<String> reportedCorrelationCaps = new HashSet<>();
    private long lastObservedTick = Long.MIN_VALUE;

    public MiningDiagnosticGateStateStore() {
        this(MiningDiagnosticSessionBudget.SESSION_HARD_CAP);
    }

    MiningDiagnosticGateStateStore(int maximumTrackedStates) {
        if (maximumTrackedStates < 1) {
            throw new IllegalArgumentException("maximumTrackedStates must be positive");
        }
        this.maximumTrackedStates = maximumTrackedStates;
    }

    public synchronized boolean resetIfTickRegressed(long tick) {
        if (lastObservedTick != Long.MIN_VALUE && tick < lastObservedTick) {
            reset();
            lastObservedTick = tick;
            return true;
        }
        lastObservedTick = tick;
        return false;
    }

    public synchronized MiningDiagnosticGateState stateFor(String key,
                                                            long tick,
                                                            long monotonicNanos,
                                                            boolean retainNewState) {
        MiningDiagnosticGateState existing = states.get(key);
        if (existing != null) {
            return existing;
        }
        MiningDiagnosticGateState created = new MiningDiagnosticGateState(tick, monotonicNanos);
        if (retainNewState && states.size() < maximumTrackedStates) {
            states.put(key, created);
        }
        return created;
    }

    public synchronized boolean claimCorrelationCapReport(String correlationKey) {
        return reportedCorrelationCaps.add(correlationKey);
    }

    public synchronized void reset() {
        states.clear();
        reportedCorrelationCaps.clear();
        lastObservedTick = Long.MIN_VALUE;
    }

    public synchronized int trackedStateCount() {
        return states.size();
    }

    public synchronized boolean containsState(String key) {
        return states.containsKey(key);
    }

}
