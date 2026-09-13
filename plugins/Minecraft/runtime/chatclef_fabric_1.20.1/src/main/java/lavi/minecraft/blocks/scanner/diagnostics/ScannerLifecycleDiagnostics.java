//#if MC == 12001
package lavi.minecraft.blocks.scanner.diagnostics;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260913_kpopmodder: Observe a finite number of ownership/publication boundaries without affecting scanner state.
public final class ScannerLifecycleDiagnostics {
    private int attempts;
    public void observe(String reason, long generation, long revision, String detail) {
        try {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> observeEligible(reason, generation, revision, detail));
        } catch (RuntimeException | LinkageError ignored) { /* observer failure never changes the selected outcome */ }
    }
    private void observeEligible(String reason, long generation, long revision, String detail) {
        if (attempts >= 128) return;
        attempts++;
        try {
            ChatClefDiagnostics.logEvent("BLOCK_SCANNER_LIFETIME", "BOUNDARY", reason, null,
                    "lifetimeGeneration", generation, "contentRevision", revision, "detail", detail,
                    "attempt", attempts, "attemptLimit", 128, "filePersistence", "NOT_VERIFIED");
        } catch (RuntimeException | LinkageError ignored) { /* observer failure never changes the selected outcome */ }
    }
}
//#endif
