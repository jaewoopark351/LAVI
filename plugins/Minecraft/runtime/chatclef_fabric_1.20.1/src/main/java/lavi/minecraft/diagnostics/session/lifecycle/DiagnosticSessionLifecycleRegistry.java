package lavi.minecraft.diagnostics.session.lifecycle;

import lavi.minecraft.diagnostics.session.lifecycle.mode.DiagnosticModeLifecycleNotifier;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticSessionObserverRegistry;
import lavi.minecraft.diagnostics.session.lifecycle.snapshot.DiagnosticLifecycleSnapshotAggregator;

//20260831_kpopmodder: Keep a fixed bounded set of diagnostics-only lifecycle collaborators.
public final class DiagnosticSessionLifecycleRegistry {
    private final DiagnosticSessionObserverRegistry observers =
            new DiagnosticSessionObserverRegistry();
    private final DiagnosticModeLifecycleNotifier modeNotifier =
            new DiagnosticModeLifecycleNotifier(observers::snapshot);
    private final DiagnosticLifecycleSnapshotAggregator snapshots =
            new DiagnosticLifecycleSnapshotAggregator(observers::snapshot);

    public void register(DiagnosticSessionLifecycleObserver observer) {
        observers.register(observer);
    }

    public void notifyBeforeModeOff() {
        modeNotifier.notifyBeforeModeOff();
    }

    public void notifyAfterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        modeNotifier.notifyAfterCleanTeardownSnapshotAttempt(emissionCallsReturned);
    }

    public Object[] finalSnapshotFields() {
        return snapshots.fields();
    }
}
