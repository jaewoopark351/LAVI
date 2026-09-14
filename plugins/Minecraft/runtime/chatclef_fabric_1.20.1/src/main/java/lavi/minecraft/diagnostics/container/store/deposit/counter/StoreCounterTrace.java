package lavi.minecraft.diagnostics.container.store.deposit.counter;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;

//20260914_kpopmodder: Forward frozen counter facts through existing finite first/detail/terminal reservations.
public final class StoreCounterTrace {
    public static final StoreCounterTrace NOOP = new StoreCounterTrace(ObservationScope.NOOP, "UNBOUND", new Object[0]);
    private final ObservationScope scope;
    private final String role;
    private final Object[] context;

    public StoreCounterTrace(ObservationScope scope, String role, Object[] context) {
        this.scope = scope == null ? ObservationScope.NOOP : scope;
        this.role = role;
        this.context = context.clone();
    }

    public boolean current() {
        if (this == NOOP || scope == ObservationScope.NOOP) return false;
        try { return scope.isCurrent(); }
        catch (RuntimeException | LinkageError unavailable) { return false; }
    }
    public String role() { return role; }
    public boolean scopeAdmitted() { return scope != ObservationScope.NOOP; }

    public void record(String boundary, String reason, Object... fields) {
        if (this == NOOP || scope == ObservationScope.NOOP) return;
        try {
            if (!ChatClefDiagnostics.isBoundaryEnabled() || !scope.isCurrent()) return;
            // Numeric values and runtime IDs are payload, not new reservation signatures.
            String fingerprint = role + ":" + reason;
            for (int i = 0; i + 1 < fields.length; i += 2) {
                if ("counterAfter".equals(fields[i]) || "deltaApplied".equals(fields[i]))
                    fingerprint += ":" + fields[i] + "=" + fields[i + 1];
            }
            scope.record("STORE_COUNTER_" + boundary, reason, fingerprint, false,
                    StoreCounterFields.mergeContext(fields, context));
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_OBSERVATION_" + failure.getClass().getSimpleName());
        }
    }

    public void close(String reason, Object... fields) {
        if (this == NOOP || scope == ObservationScope.NOOP) return;
        try {
            if (scope.isCurrent()) scope.close(reason, StoreCounterFields.mergeContext(fields, context));
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_CLOSE_" + failure.getClass().getSimpleName());
        }
    }
}
