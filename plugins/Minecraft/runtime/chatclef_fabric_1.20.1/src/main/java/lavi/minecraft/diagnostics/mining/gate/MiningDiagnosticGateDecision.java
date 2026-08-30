package lavi.minecraft.diagnostics.mining.gate;

import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticAdmission;

//20260830_kpopmodder: Carry one immutable mining gate admission or suppression result.
public final class MiningDiagnosticGateDecision {
    public final boolean emit;
    public final boolean summary;
    public final int suppressedCount;
    public final long firstObservedTick;
    public final long lastObservedTick;
    public final boolean reportCorrelationCap;
    public final boolean reportSessionCap;
    public final MiningDiagnosticAdmission admission;

    private MiningDiagnosticGateDecision(boolean emit,
                                         boolean summary,
                                         int suppressedCount,
                                         long firstObservedTick,
                                         long lastObservedTick,
                                         boolean reportCorrelationCap,
                                         boolean reportSessionCap,
                                         MiningDiagnosticAdmission admission) {
        this.emit = emit;
        this.summary = summary;
        this.suppressedCount = suppressedCount;
        this.firstObservedTick = firstObservedTick;
        this.lastObservedTick = lastObservedTick;
        this.reportCorrelationCap = reportCorrelationCap;
        this.reportSessionCap = reportSessionCap;
        this.admission = admission;
    }

    static MiningDiagnosticGateDecision emitted(boolean summary,
                                                int suppressedCount,
                                                long firstObservedTick,
                                                long lastObservedTick,
                                                MiningDiagnosticAdmission admission) {
        return new MiningDiagnosticGateDecision(
                true, summary, suppressedCount, firstObservedTick, lastObservedTick,
                false, false, admission);
    }

    static MiningDiagnosticGateDecision suppressed(MiningDiagnosticGateState state,
                                                   MiningDiagnosticAdmission admission) {
        return suppressed(state, admission, false, admission != null && admission.reportSessionCap());
    }

    static MiningDiagnosticGateDecision suppressed(MiningDiagnosticGateState state,
                                                   MiningDiagnosticAdmission admission,
                                                   boolean reportCorrelationCap,
                                                   boolean reportSessionCap) {
        return new MiningDiagnosticGateDecision(
                false, false, state.suppressedCount, state.firstObservedTick, state.lastObservedTick,
                reportCorrelationCap, reportSessionCap, admission);
    }
}
