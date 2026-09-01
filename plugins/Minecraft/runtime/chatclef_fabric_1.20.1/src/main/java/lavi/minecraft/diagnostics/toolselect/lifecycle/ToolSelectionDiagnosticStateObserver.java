package lavi.minecraft.diagnostics.toolselect.lifecycle;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.Objects;

//20260831_kpopmodder: Clear one tool-selection diagnostic owner's bounded state before OFF publication.
public final class ToolSelectionDiagnosticStateObserver
        implements DiagnosticSessionLifecycleObserver {
    private final Runnable clearAction;

    public ToolSelectionDiagnosticStateObserver(Runnable clearAction) {
        this.clearAction = Objects.requireNonNull(clearAction, "clearAction");
    }

    @Override
    public void beforeModeOff() {
        clearAction.run();
    }
}
