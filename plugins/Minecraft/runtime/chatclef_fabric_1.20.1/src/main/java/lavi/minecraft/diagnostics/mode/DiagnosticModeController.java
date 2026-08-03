package lavi.minecraft.diagnostics.mode;

//20260803_kpopmodder: Split diagnostic output mode ownership out of the ChatClef diagnostics facade.
public final class DiagnosticModeController {
    private volatile DiagnosticOutputMode outputMode;

    public DiagnosticModeController(DiagnosticOutputMode outputMode) {
        this.outputMode = outputMode;
    }

    public DiagnosticOutputMode current() {
        return outputMode;
    }

    public boolean isOff() {
        return outputMode == DiagnosticOutputMode.OFF;
    }

    public boolean isVerboseEnabled() {
        return outputMode == DiagnosticOutputMode.VERBOSE;
    }

    public boolean isBoundaryEnabled() {
        return outputMode != DiagnosticOutputMode.OFF;
    }

    public void setBoundaryEnabled(boolean enabled) {
        outputMode = enabled ? DiagnosticOutputMode.BOUNDARY : DiagnosticOutputMode.OFF;
    }
}
