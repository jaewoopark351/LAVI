package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

//20260913_kpopmodder: Captured lifecycle callbacks cannot revive a disabled diagnostic owner.
final class DiagnosticOwnerLifecycleObserver implements DiagnosticSessionLifecycleObserver {
    private final DiagnosticOwnerRegistration owner;
    private final DiagnosticSessionLifecycleObserver observer;

    DiagnosticOwnerLifecycleObserver(DiagnosticOwnerRegistration owner, DiagnosticSessionLifecycleObserver observer) {
        this.owner = owner;
        this.observer = observer;
    }

    @Override
    public void beforeModeOff() {
        owner.runIfAvailable(observer::beforeModeOff);
    }

    @Override
    public void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        owner.runIfAvailable(() -> observer.afterCleanTeardownSnapshotAttempt(emissionCallsReturned));
    }

    @Override
    public Object[] finalSnapshotFields() {
        return owner.callIfAvailable(observer::finalSnapshotFields, new Object[0]);
    }
}
