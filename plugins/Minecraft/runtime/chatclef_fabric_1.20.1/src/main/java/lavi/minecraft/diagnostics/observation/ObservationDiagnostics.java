package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.emission.ObservationBoundaryEmitter;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;
import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.observation.state.ObservationRegistry;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;

/** Eligibility facade. Callers supply already-decided facts, never gameplay policy callbacks. */
public final class ObservationDiagnostics {
    private static final ObservationRegistry REGISTRY = new ObservationRegistry();
    //20260913_kpopmodder: A rejected lifecycle binding permanently disables this diagnostic owner.
    private static final DiagnosticOwnerRegistration LIFECYCLE =
            new DiagnosticOwnerRegistration("resource-observation", REGISTRY::invalidate);
    private static final DiagnosticSessionLifecycleObserver OBSERVER = new DiagnosticSessionLifecycleObserver() {
        @Override public void beforeModeOff() { REGISTRY.invalidate(); }
        @Override public void afterCleanTeardownSnapshotAttempt(boolean returned) { REGISTRY.invalidate(); }
        @Override public Object[] finalSnapshotFields() { return REGISTRY.snapshot(); }
    };
    private ObservationDiagnostics() { }

    public static void installLifecycleObserver() {
        ChatClefDiagnostics.registerSessionLifecycleOwner(LIFECYCLE, OBSERVER);
    }

    public static ObservationActivation captureActivation(Object instance, Object world) {
        try {
            return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> LIFECYCLE.callIfAvailable(() ->
                    REGISTRY.capture(ChatClefDiagnostics.diagnosticActivationEpoch(), instance, world), null), null);
        } catch (RuntimeException | LinkageError failure) {
            captureFailed("activation", failure.getClass().getSimpleName());
            return null;
        }
    }
    public static ObservationScope open(ObservationActivation activation, String domain, String operationKey,
                                        Object... contextFields) {
        if (!isCurrent(activation)) return ObservationScope.NOOP;
        try {
            return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> LIFECYCLE.callIfAvailable(() -> isCurrent(activation)
                    ? REGISTRY.open(activation, ObservationFields.text(domain), ObservationFields.text(operationKey),
                        ObservationFields.copy(contextFields)) : ObservationScope.NOOP, ObservationScope.NOOP), ObservationScope.NOOP);
        } catch (RuntimeException | LinkageError failure) {
            captureFailed(domain, failure.getClass().getSimpleName());
            return ObservationScope.NOOP;
        }
    }
    public static boolean isCurrent(ObservationActivation activation) {
        return LIFECYCLE.isAvailable() && activation != null && ChatClefDiagnostics.isBoundaryEnabled()
                && activation.live(ChatClefDiagnostics.diagnosticActivationEpoch());
    }
    public static void record(ObservationScope scope, String event, String reason, String fingerprint,
                              boolean terminal, Object... fields) {
        if (!scope.isCurrent()) {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(REGISTRY::recordRejected));
            return;
        }
        try {
            Object[] frozen = ObservationFields.copy(fields);
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() -> {
                if (!scope.isCurrent()) { REGISTRY.recordRejected(); return; }
                ObservationEmission emission = scope.ledger().capture(ObservationFields.text(event),
                        ObservationFields.text(reason), ObservationFields.text(fingerprint), terminal,
                        ChatClefDiagnostics.currentClientTickId(), System.nanoTime(), frozen);
                if (emission != null) ObservationBoundaryEmitter.emit(scope, emission);
            }));
        } catch (RuntimeException | LinkageError failure) {
            captureFailed(scope.domain(), failure.getClass().getSimpleName());
        }
    }
    public static void retireOtherOperations(ObservationActivation activation, String domain, String keepOperationKey) {
        if (!isCurrent(activation)) return;
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() -> {
            for (ObservationScope scope : REGISTRY.otherOperations(activation, domain, keepOperationKey))
                scope.close("NEW_OPERATION_OBSERVED", "replacementOperationKey", keepOperationKey);
        }));
    }

    //20260913_kpopmodder: Retained handles must recheck eligibility inside the same lease as their write.
    public static void pin(ObservationScope scope, String slot, Object... fields) {
        try {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() -> {
                if (scope.isCurrent()) scope.ledger().pin(ObservationFields.text(slot), ObservationFields.freeze(fields));
            }));
        } catch (RuntimeException | LinkageError failure) {
            captureFailed(scope.domain(), failure.getClass().getSimpleName());
        }
    }
    public static void observeWorld(Object world) {
        LIFECYCLE.runIfAvailable(() -> REGISTRY.observeWorld(world));
    }
    public static void captureFailed(String domain, String exceptionType) {
        try {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> LIFECYCLE.runIfAvailable(() ->
                    REGISTRY.captureFailed(ObservationFields.text(domain), ObservationFields.text(exceptionType))));
        } catch (RuntimeException | LinkageError ignored) {
            // A failed diagnostic class initialization must not recursively report through itself.
        }
    }
    public static DiagnosticSessionLifecycleObserver lifecycleObserver() {
        return OBSERVER;
    }
}
