package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.registration.emission.DiagnosticRegistrationFailureEmitter;

//20260913_kpopmodder: Use one process-bounded emergency sink with no ChatClef initialization dependency.
public final class DiagnosticRegistrationFailures {
    private static final DiagnosticRegistrationFailureEmitter EMITTER = new DiagnosticRegistrationFailureEmitter(
            line -> System.err.println(line));

    private DiagnosticRegistrationFailures() { }

    static void report(String owner, DiagnosticObserverRegistrationResult result, String boundary, String reason) {
        EMITTER.report(owner, result, boundary, reason);
    }

    public static void reportInitializationFailure(String owner, String boundary, Throwable failure) {
        EMITTER.report(owner, null, boundary, failure == null ? "UNKNOWN_FAILURE" : failure.getClass().getName());
    }
}
