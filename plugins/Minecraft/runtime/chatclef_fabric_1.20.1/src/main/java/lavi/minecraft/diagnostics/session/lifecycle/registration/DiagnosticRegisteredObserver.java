package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

//20260913_kpopmodder: Retain duplicate identity separately from availability-guarded dispatch.
final class DiagnosticRegisteredObserver {
    private final DiagnosticSessionLifecycleObserver observer;
    private final DiagnosticOwnerRegistration owner;
    private final DiagnosticSessionLifecycleObserver callback;

    DiagnosticRegisteredObserver(DiagnosticSessionLifecycleObserver observer, DiagnosticOwnerRegistration owner) {
        this.observer = observer;
        this.owner = owner;
        this.callback = owner == null ? observer : new DiagnosticOwnerLifecycleObserver(owner, observer);
    }

    DiagnosticSessionLifecycleObserver observer() { return observer; }
    DiagnosticOwnerRegistration owner() { return owner; }
    DiagnosticSessionLifecycleObserver callback() { return callback; }
}
