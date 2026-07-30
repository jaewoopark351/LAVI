package lavi.minecraft.integration.carryon;

import adris.altoclef.Debug;

//20260730_kpopmodder: Keep Carry On diagnostic output in one LAVI-owned logging boundary.
public final class CarryOnDiagnosticLogger {
    private CarryOnDiagnosticLogger() {
    }

    public static void log(CarryOnSnapshot snapshot) {
        Debug.logWarning(CarryOnDiagnosticFormatter.format(snapshot));
    }
}
