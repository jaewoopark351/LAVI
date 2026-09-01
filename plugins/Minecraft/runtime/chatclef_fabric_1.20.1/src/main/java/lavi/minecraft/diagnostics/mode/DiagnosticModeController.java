package lavi.minecraft.diagnostics.mode;

//20260803_kpopmodder: Split diagnostic output mode ownership out of the ChatClef diagnostics facade.
public final class DiagnosticModeController {
    private volatile DiagnosticOutputMode outputMode;
    private volatile boolean offTransitionRequested;
    private long modeEpoch;

    public DiagnosticModeController(DiagnosticOutputMode outputMode) {
        this.outputMode = outputMode;
    }

    public DiagnosticOutputMode current() {
        return outputMode;
    }

    public boolean isOff() {
        return offTransitionRequested || outputMode == DiagnosticOutputMode.OFF;
    }

    public boolean isVerboseEnabled() {
        return !offTransitionRequested && outputMode == DiagnosticOutputMode.VERBOSE;
    }

    public boolean isBoundaryEnabled() {
        return !offTransitionRequested && outputMode != DiagnosticOutputMode.OFF;
    }

    public synchronized void setBoundaryEnabled(boolean enabled) {
        completeBoundaryTransition(enabled);
    }

    public void beginOffTransition() {
        offTransitionRequested = true;
    }

    public synchronized void completeBoundaryTransition(boolean enabled) {
        outputMode = enabled ? DiagnosticOutputMode.BOUNDARY : DiagnosticOutputMode.OFF;
        offTransitionRequested = false;
        if (modeEpoch < Long.MAX_VALUE) {
            modeEpoch++;
        }
    }

    public synchronized long modeEpoch() {
        return modeEpoch;
    }
}
