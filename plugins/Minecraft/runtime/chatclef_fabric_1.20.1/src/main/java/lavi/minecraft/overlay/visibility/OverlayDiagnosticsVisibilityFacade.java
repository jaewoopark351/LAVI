package lavi.minecraft.overlay.visibility;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260805_kpopmodder: Isolate the existing overlay-to-diagnostics toggle side effect without changing its behavior.
final class OverlayDiagnosticsVisibilityFacade {
    void setBoundaryEnabled(boolean visible) {
        ChatClefDiagnostics.setBoundaryEnabled(visible);
    }
}
