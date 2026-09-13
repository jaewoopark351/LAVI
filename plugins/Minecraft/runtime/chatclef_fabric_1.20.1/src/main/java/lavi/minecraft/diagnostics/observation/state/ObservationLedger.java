package lavi.minecraft.diagnostics.observation.state;

import lavi.minecraft.diagnostics.observation.format.ObservationPayloadFormatter;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision.RejectionReason;
import lavi.minecraft.diagnostics.observation.state.checkpoint.ObservationCheckpointEventPolicy;

//20260913_kpopmodder: Coordinate bounded evidence, output accounting, and payload formatting under one diagnostic lock.
public final class ObservationLedger {
    public static final int FIRST_LIMIT = 32;
    public static final int DETAIL_LIMIT = 256;
    private final ObservationEvidenceMemory evidence = new ObservationEvidenceMemory(FIRST_LIMIT);
    private final ObservationOutputAccounting output = new ObservationOutputAccounting(DETAIL_LIMIT);

    public synchronized void pin(String slot, String frozen) {
        evidence.pin(slot, frozen);
    }

    public synchronized ObservationEmission capture(String event, String reason, String fingerprint,
                                                    boolean terminal, long tick, long nanos, String fields) {
        return capture(event, reason, fingerprint, terminal, tick, nanos, new Object[]{"detail", fields});
    }
    public synchronized ObservationEmission capture(String event, String reason, String fingerprint,
                                                    boolean terminal, long tick, long nanos, Object[] fields) {
        output.captureStarted();
        String snapshot = ObservationPayloadFormatter.evidenceSnapshot(event, reason, tick, fields);
        boolean newFirst = evidence.remember(event, reason, fingerprint, terminal, snapshot);
        String tier = output.admit(terminal, newFirst, ObservationCheckpointEventPolicy.allows(event), tick, nanos);
        return tier == null ? null : ObservationPayloadFormatter.emission(event, reason, tier, tick, nanos, fields, output, evidence);
    }

    public synchronized void settle(boolean wasAdmitted, boolean emissionReturned, String outcome) {
        output.settle(wasAdmitted, emissionReturned, outcome);
    }

    public synchronized void settle(ObservationEmission emission, boolean wasAdmitted, boolean emissionReturned,
                                    String outcome, RejectionReason reason) {
        output.settle(emission, wasAdmitted, emissionReturned, outcome, reason);
    }

    public synchronized Object[] summary() {
        return ObservationPayloadFormatter.summary(output, evidence);
    }
}
