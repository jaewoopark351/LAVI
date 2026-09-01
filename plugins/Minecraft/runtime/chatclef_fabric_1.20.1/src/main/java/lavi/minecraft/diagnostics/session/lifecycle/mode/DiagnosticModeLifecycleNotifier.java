package lavi.minecraft.diagnostics.session.lifecycle.mode;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

//20260831_kpopmodder: Dispatch only mode/teardown invalidation callbacks.
public final class DiagnosticModeLifecycleNotifier {
    private final Supplier<List<DiagnosticSessionLifecycleObserver>> observers;

    public DiagnosticModeLifecycleNotifier(
            Supplier<List<DiagnosticSessionLifecycleObserver>> observers) {
        this.observers = Objects.requireNonNull(observers, "observers");
    }

    public void notifyBeforeModeOff() {
        for (DiagnosticSessionLifecycleObserver observer : observers.get()) {
            try {
                observer.beforeModeOff();
            } catch (RuntimeException | LinkageError ignored) {
                // Mode publication cannot depend on diagnostics-only cleanup.
            }
        }
    }

    public void notifyAfterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        for (DiagnosticSessionLifecycleObserver observer : observers.get()) {
            try {
                observer.afterCleanTeardownSnapshotAttempt(emissionCallsReturned);
            } catch (RuntimeException | LinkageError ignored) {
                // Final evidence was already captured; cleanup remains best-effort.
            }
        }
    }
}
