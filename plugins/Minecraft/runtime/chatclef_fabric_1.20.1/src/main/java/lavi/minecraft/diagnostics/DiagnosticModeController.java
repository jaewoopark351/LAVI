package lavi.minecraft.diagnostics;

//20260803_kpopmodder: Split diagnostic output mode ownership out of the ChatClef diagnostics facade.
final class DiagnosticModeController {
    private volatile DiagnosticOutputMode outputMode;

    DiagnosticModeController(DiagnosticOutputMode outputMode) {
        this.outputMode = outputMode;
    }

    DiagnosticOutputMode current() {
        return outputMode;
    }

    boolean isOff() {
        return outputMode == DiagnosticOutputMode.OFF;
    }

    boolean isVerboseEnabled() {
        return outputMode == DiagnosticOutputMode.VERBOSE;
    }

    boolean isBoundaryEnabled() {
        return outputMode != DiagnosticOutputMode.OFF;
    }

    void setBoundaryEnabled(boolean enabled) {
        outputMode = enabled ? DiagnosticOutputMode.BOUNDARY : DiagnosticOutputMode.OFF;
    }
}
