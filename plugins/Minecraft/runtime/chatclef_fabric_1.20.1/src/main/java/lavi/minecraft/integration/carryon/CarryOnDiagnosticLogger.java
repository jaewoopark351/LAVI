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
        return capabilityFailure(snapshot.stateBefore())
                || capabilityFailure(snapshot.stateAfter())
                || snapshot.terminalReason() == CarryOnTerminalReason.CAPABILITY_INCOMPATIBLE
                || snapshot.terminalReason() == CarryOnTerminalReason.STATE_UNREADABLE
                || snapshot.terminalReason() == CarryOnTerminalReason.OBSERVATION_FAILED;
    }

    private static boolean capabilityFailure(CarryOnObservation observation) {
        if (observation == null) {
            return false;
        }
        return observation.state() == CarryOnCarryState.INCOMPATIBLE
                || observation.state() == CarryOnCarryState.STATE_UNREADABLE
                || observation.state() == CarryOnCarryState.OBSERVATION_FAILED;
    }
}
