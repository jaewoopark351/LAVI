package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.observation.state.ObservationLedger;

/** Handle only: state, formatting, and shared emission are independently owned. */
public final class ObservationScope {
    public static final ObservationScope NOOP = new ObservationScope(null, "none", "none", "");
    private final ObservationActivation activation;
    private final String domain, operationKey;
    private final Object[] context;
    private final ObservationLedger ledger = new ObservationLedger();
    private volatile boolean closed;

    public ObservationScope(ObservationActivation activation, String domain, String operationKey, String context) {
        this(activation, domain, operationKey, new Object[]{"context", context});
    }
    public ObservationScope(ObservationActivation activation, String domain, String operationKey, Object[] context) {
        this.activation = activation;
        this.domain = domain;
        this.operationKey = operationKey;
        this.context = context.clone();
    }
    public void record(String event, String reason, String fingerprint, boolean terminal, Object... fields) {
        ObservationDiagnostics.record(this, event, reason, fingerprint, terminal, fields);
    }
    public void pin(String slot, Object... fields) {
        ObservationDiagnostics.pin(this, slot, fields);
    }
    public void close(String reason, Object... fields) {
        if (!isCurrent()) return;
        record("OBSERVATION_SCOPE_CLOSED", reason, "closed", true, fields);
        closed = true;
    }
    public boolean isCurrent() { return !closed && activation != null && activation.isCurrent(); }
    public ObservationActivation activation() { return activation; }
    public String domain() { return domain; }
    public String operationKey() { return operationKey; }
    public Object[] context() { return context.clone(); }
    public ObservationLedger ledger() { return ledger; }
}
