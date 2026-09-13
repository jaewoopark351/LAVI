package lavi.minecraft.diagnostics.observation.state.checkpoint;

import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision.RejectionReason;

//20260913_kpopmodder: A real ordinary budget rejection opens one finite, coarse summary allowance per scope.
public final class ObservationCheckpointState {
    public static final int LIMIT = 32;
    public static final long INTERVAL_TICKS = 200;
    public static final long INTERVAL_NANOS = 10_000_000_000L;
    private boolean ordinaryRejected;
    private String rejectionReason = "NOT_OBSERVED";
    private long rejectionTick = -1;
    private long lastTick;
    private long lastNanos;
    private int attempted;
    private int admitted;
    private int callsReturned;

    public void settled(ObservationEmission emission, boolean wasAdmitted, boolean emissionReturned,
                        RejectionReason reason) {
        if ("SUMMARY".equals(emission.tier())) {
            if (wasAdmitted) admitted++;
            if (emissionReturned) callsReturned++;
        } else if (!ordinaryRejected && "DETAIL".equals(emission.tier()) && !wasAdmitted
                && (reason == RejectionReason.ORDINARY_CEILING_REACHED
                    || reason == RejectionReason.SHARED_HARD_CAP_REACHED)) {
            ordinaryRejected = true;
            rejectionReason = reason.name();
            rejectionTick = emission.tick();
            lastTick = emission.tick();
            lastNanos = emission.nanos();
        }
    }

    public boolean active() { return ordinaryRejected; }

    public boolean reserve(long tick, long nanos) {
        if (!ordinaryRejected || attempted >= LIMIT || tick - lastTick < INTERVAL_TICKS
                || nanos - lastNanos < INTERVAL_NANOS) return false;
        attempted++;
        lastTick = tick;
        lastNanos = nanos;
        return true;
    }

    public Object[] fields() {
        return new Object[]{"ordinaryBudgetRejectionObserved", ordinaryRejected,
                "ordinaryBudgetRejectionReason", rejectionReason, "ordinaryBudgetRejectionTick", rejectionTick,
                "summaryAttemptedCount", attempted, "summaryAdmittedCount", admitted,
                "summaryEmissionCallsReturned", callsReturned, "summaryAttemptLimit", LIMIT,
                "summaryAllowanceExhausted", attempted >= LIMIT, "summaryRemainingAttempts", LIMIT - attempted,
                "summaryIntervalTicks", INTERVAL_TICKS, "summaryIntervalNanos", INTERVAL_NANOS,
                "summaryFilePersistence", "NOT_VERIFIED"};
    }
}
