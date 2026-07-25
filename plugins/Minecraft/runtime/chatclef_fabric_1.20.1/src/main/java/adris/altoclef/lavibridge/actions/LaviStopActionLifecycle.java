package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this lifecycle wrapper for stop action state transitions.

import adris.altoclef.lavibridge.LaviActionRegistry;

import java.util.Map;

public class LaviStopActionLifecycle {

    private final LaviActionRegistry actionRegistry;

    public LaviStopActionLifecycle(LaviActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    public String start(Map<String, Object> request) {
        Map<String, Object> action = actionRegistry.createAction("stop", "stop", request);
        String actionId = (String) action.get("action_id");
        actionRegistry.markRunning(actionId);
        return actionId;
    }

    public void succeeded(String actionId) {
        actionRegistry.markSucceeded(actionId, "Stop requested.");
    }

    public Map<String, Object> currentSnapshot() {
        return actionRegistry.currentActionSnapshotOnly();
    }
}
