package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this canceller to isolate previous-action cancellation from stop action creation.

import adris.altoclef.lavibridge.LaviActionRegistry;

import java.util.Map;

public class LaviRunningActionCanceller {

    private final LaviActionRegistry actionRegistry;

    public LaviRunningActionCanceller(LaviActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    public Map<String, Object> cancelForStopRequest() {
        return actionRegistry.cancelCurrentIfRunning("Cancelled by stop request.");
    }
}
