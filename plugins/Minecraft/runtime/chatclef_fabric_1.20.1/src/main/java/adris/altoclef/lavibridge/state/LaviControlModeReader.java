package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader to isolate MANUAL/AI/PAUSED mode decisions.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;

public class LaviControlModeReader {

    private final AltoClef mod;
    private final LaviActionRegistry actionRegistry;

    public LaviControlModeReader(AltoClef mod, LaviActionRegistry actionRegistry) {
        this.mod = mod;
        this.actionRegistry = actionRegistry;
    }

    public String controlMode() {
        if (mod == null || mod.isPaused()) {
            return "PAUSED";
        }
        if (actionRegistry.hasRunningAction()) {
            return "AI";
        }
        if (mod.getUserTaskChain() != null
                && mod.getUserTaskChain().isActive()
                && !mod.getUserTaskChain().isRunningIdleTask()) {
            return "AI";
        }
        return "MANUAL";
    }
}
