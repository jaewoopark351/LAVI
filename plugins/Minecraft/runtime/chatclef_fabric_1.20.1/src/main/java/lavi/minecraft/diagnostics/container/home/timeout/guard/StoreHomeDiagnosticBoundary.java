package lavi.minecraft.diagnostics.container.home.timeout.guard;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260829_kpopmodder: Gate boundary diagnostics without changing observed exception behavior.
public final class StoreHomeDiagnosticBoundary {
    private StoreHomeDiagnosticBoundary() {
    }

    public static void runIfEnabled(Runnable action) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(action);
    }
}
