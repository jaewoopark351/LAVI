package lavi.minecraft.diagnostics.session.lifecycle.registration;

import java.util.function.Supplier;

//20260913_kpopmodder: Serialize diagnostic writes and permanent invalidation for one owner only.
public final class DiagnosticOwnerRegistration {
    private final String ownerId;
    private final Runnable diagnosticCleanup;
    private volatile boolean available;
    private volatile DiagnosticObserverRegistrationResult result = new DiagnosticObserverRegistrationResult(
            DiagnosticObserverRegistrationStatus.UNREGISTERED, 0, DiagnosticSessionObserverRegistry.MAX_OBSERVERS, 0);
    private boolean disabled;

    public DiagnosticOwnerRegistration(String ownerId, Runnable diagnosticCleanup) {
        this.ownerId = ownerId;
        this.diagnosticCleanup = diagnosticCleanup;
    }

    public String ownerId() { return ownerId; }
    public boolean isAvailable() { return available; }
    public DiagnosticObserverRegistrationResult registrationResult() { return result; }

    boolean hasValidOwnerId() { return ownerId != null && !ownerId.isBlank() && diagnosticCleanup != null; }

    DiagnosticObserverRegistrationResult registerWith(Supplier<DiagnosticObserverRegistrationResult> registration) {
        DiagnosticObserverRegistrationResult registered;
        boolean reportFailure = false;
        synchronized (this) {
            if (disabled) {
                return result;
            }
            // The owner lock precedes the registry lock; registry code never calls cleanup or a sink.
            registered = registration.get();
            result = registered;
            available = registered.accepted();
            if (!available) {
                disabled = true;
                cleanup();
                reportFailure = true;
            }
        }
        if (reportFailure) {
            DiagnosticRegistrationFailures.report(ownerId, registered, "REGISTER_OWNER", registered.status().name());
        }
        return registered;
    }

    public synchronized void runIfAvailable(Runnable action) {
        if (available) {
            action.run();
        }
    }

    public synchronized <T> T callIfAvailable(Supplier<T> action, T fallback) {
        return available ? action.get() : fallback;
    }

    public void disable(String reason) {
        synchronized (this) {
            if (disabled) {
                return;
            }
            available = false;
            disabled = true;
            cleanup();
        }
        DiagnosticRegistrationFailures.report(ownerId, result, "DISABLE_OWNER", reason);
    }

    private void cleanup() {
        try {
            if (diagnosticCleanup != null) {
                diagnosticCleanup.run();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Unavailable stays permanent even when diagnostics-only cleanup cannot complete.
        }
    }
}
