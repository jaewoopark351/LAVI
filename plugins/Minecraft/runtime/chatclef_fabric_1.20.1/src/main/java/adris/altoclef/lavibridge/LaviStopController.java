package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class so LAVI stop requests use the existing AltoClef stop path.

import adris.altoclef.AltoClef;

public class LaviStopController {

    private final AltoClef mod;

    public LaviStopController(AltoClef mod) {
        this.mod = mod;
    }

    public void stopAutomation() {
        mod.stop();
    }
}
