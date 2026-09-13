package lavi.minecraft.diagnostics.toolselect.lifecycle;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

//20260831_kpopmodder: Clear one tool-selection diagnostic owner's bounded state before OFF publication.
public final class ToolSelectionDiagnosticStateObserver
        implements DiagnosticSessionLifecycleObserver {
    private final Runnable clearAction;
    private final AtomicLong generation = new AtomicLong();

    public ToolSelectionDiagnosticStateObserver(Runnable clearAction) {
        this.clearAction = Objects.requireNonNull(clearAction, "clearAction");
    }

    @Override
    public void beforeModeOff() {
        generation.incrementAndGet();
        clearAction.run();
    }

    @Override
    public void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        generation.incrementAndGet();
        clearAction.run();
    }

    public long generation() {
        return generation.get();
    }
}
