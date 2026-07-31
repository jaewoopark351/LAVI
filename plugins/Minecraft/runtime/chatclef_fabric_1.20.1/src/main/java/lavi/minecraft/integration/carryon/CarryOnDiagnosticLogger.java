package lavi.minecraft.integration.carryon;

import adris.altoclef.Debug;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260730_kpopmodder: Keep Carry On diagnostic output in one LAVI-owned logging boundary.
public final class CarryOnDiagnosticLogger {
    private CarryOnDiagnosticLogger() {
    }

    public static void log(CarryOnSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        String formatted = CarryOnDiagnosticFormatter.format(snapshot);
        if (ChatClefDiagnostics.isVerboseEnabled()) {
            ChatClefDiagnostics.logVerboseLine(formatted);
            return;
        }
        if (warningSnapshot(snapshot)) {
            Debug.logWarning(formatted);
        }
    }

    private static boolean warningSnapshot(CarryOnSnapshot snapshot) {
        return CarryOnObservationClassifier.capabilityFailure(snapshot.stateBefore())
                || CarryOnObservationClassifier.capabilityFailure(snapshot.stateAfter())
                || CarryOnObservationClassifier.warningTerminal(snapshot.terminalReason());
    }
}
