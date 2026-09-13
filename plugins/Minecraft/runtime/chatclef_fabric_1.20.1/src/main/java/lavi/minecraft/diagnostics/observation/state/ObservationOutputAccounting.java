package lavi.minecraft.diagnostics.observation.state;

import lavi.minecraft.diagnostics.observation.state.checkpoint.ObservationCheckpointState;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision.RejectionReason;

//20260913_kpopmodder: Own output admission limits and attempted/admitted/sink-result accounting only.
public final class ObservationOutputAccounting {
    private final int detailLimit;
    private final ObservationCheckpointState checkpoints = new ObservationCheckpointState();
    private long captured, attempted, admitted, returned, failures, suppressed;
    private long lastTick = Long.MIN_VALUE, lastNanos;
    private int details;
    private boolean terminalAttempted;
    private String lastOutcome = "NOT_ATTEMPTED";

    public ObservationOutputAccounting(int detailLimit) {
        this.detailLimit = detailLimit;
    }

    public void captureStarted() { captured++; }

    public String admit(boolean terminal, boolean newFirst, boolean checkpointCandidate, long tick, long nanos) {
        String tier;
        if (terminal && !terminalAttempted) {
            terminalAttempted = true;
            tier = "TERMINAL";
        } else if (newFirst) tier = "FIRST";
        else if (!terminal && checkpoints.active()) {
            if (!checkpointCandidate || !checkpoints.reserve(tick, nanos)) {
                suppressed++;
                return null;
            }
            tier = "SUMMARY";
        }
        else if (!terminal && details < detailLimit
                && (lastTick == Long.MIN_VALUE || tick - lastTick >= 200)
                && (lastNanos == 0 || nanos - lastNanos >= 10_000_000_000L)) {
            details++;
            tier = "DETAIL";
        } else {
            suppressed++;
            return null;
        }
        attempted++;
        lastTick = tick;
        lastNanos = nanos;
        return tier;
    }

    public void settle(boolean wasAdmitted, boolean emissionReturned, String outcome) {
        if (wasAdmitted) admitted++;
        if (emissionReturned) returned++;
        else failures++;
        lastOutcome = outcome;
    }

    public void settle(ObservationEmission emission, boolean wasAdmitted, boolean emissionReturned,
                       String outcome, RejectionReason reason) {
        settle(wasAdmitted, emissionReturned, outcome);
        checkpoints.settled(emission, wasAdmitted, emissionReturned, reason);
    }

    public Object[] checkpointFields() { return checkpoints.fields(); }

    public long captured() { return captured; }
    public long attempted() { return attempted; }
    public long admitted() { return admitted; }
    public long returned() { return returned; }
    public long failures() { return failures; }
    public long suppressed() { return suppressed; }
    public String lastOutcome() { return lastOutcome; }
}
