package lavi.minecraft.diagnostics.mining.gate;

//20260830_kpopmodder: Hold one retained mining correlation-and-bucket observation window.
public final class MiningDiagnosticGateState {
    public String fingerprint = "";
    public int suppressedCount;
    public long firstObservedTick;
    public long lastObservedTick;
    public long lastSummaryTick;
    public long lastSummaryNanos;

    MiningDiagnosticGateState(long tick, long monotonicNanos) {
        firstObservedTick = tick;
        lastObservedTick = tick;
        lastSummaryTick = tick;
        lastSummaryNanos = monotonicNanos;
    }

    public void incrementSuppressed() {
        if (suppressedCount < Integer.MAX_VALUE) {
            suppressedCount++;
        }
    }
}
