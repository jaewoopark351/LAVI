package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this runner to isolate AltoClef automation stop call.

import adris.altoclef.lavibridge.LaviStopController;

public class LaviStopAutomationRunner {

    private final LaviStopController stopController;

    public LaviStopAutomationRunner(LaviStopController stopController) {
        this.stopController = stopController;
    }

    public void stop() {
        stopController.stopAutomation();
    }
}
