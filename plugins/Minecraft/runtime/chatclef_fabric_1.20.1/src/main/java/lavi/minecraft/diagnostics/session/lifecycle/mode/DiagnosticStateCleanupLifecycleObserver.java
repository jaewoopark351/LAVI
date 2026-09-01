package lavi.minecraft.diagnostics.session.lifecycle.mode;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.Objects;

//20260831_kpopmodder: Invalidate one diagnostics-only state owner at OFF and clean teardown boundaries.
public final class DiagnosticStateCleanupLifecycleObserver
        implements DiagnosticSessionLifecycleObserver {
    private final Runnable clearAction;

    public DiagnosticStateCleanupLifecycleObserver(Runnable clearAction) {
        this.clearAction = Objects.requireNonNull(clearAction, "clearAction");
    }

    @Override
    public void beforeModeOff() {
        clearAction.run();
    }

    @Override
    public void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        clearAction.run();
    }
}
