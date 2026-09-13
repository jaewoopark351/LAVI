package lavi.minecraft.diagnostics.baritone.correlation;

import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.baritone.builder.BuilderTransitionSummary;

//20260913_kpopmodder: Emit frozen observations only after updating bounded in-memory provenance.
public final class BuilderTraceState {
    public final ObservationScope scope;
    public final BuilderTraceLedger ledger = new BuilderTraceLedger();
    private String adoptionFingerprint = "";

    public BuilderTraceState(ObservationScope scope) { this.scope = scope; }
    public boolean current() { return BuilderTraceRegistry.isAvailable() && scope.isCurrent(); }
    public synchronized boolean adoptionChanged(String fingerprint) {
        if (fingerprint.equals(adoptionFingerprint)) return false;
        adoptionFingerprint = fingerprint;
        return true;
    }
    public void event(String event, String reason, String fingerprint, Object... fields) {
        if (!current()) return;
        long sequence = ledger.recordIfChanged(event + ":" + reason,
                BuilderTransitionSummary.format(event, reason, fields));
        scope.record(event, reason, fingerprint, false, MiningDiagnosticEmitter.merge(new Object[]{
                "builderSequence", sequence, "pathLinkEvictions", ledger.evictions(),
                "coverageComplete", false, "coverageLimit", "RUNTIME_INJECTION_AND_FILE_OBSERVATION_UNVERIFIED",
                "captureThread", Thread.currentThread().getName(), "captureNanos", System.nanoTime(),
                "fileObservation", "UNKNOWN"
        }, fields));
    }
}
