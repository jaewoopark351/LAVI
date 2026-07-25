package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this lifecycle wrapper to keep command execution separate from action state updates.

import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.Map;

public class LaviCommandActionLifecycle {

    private final LaviActionRegistry actionRegistry;

    public LaviCommandActionLifecycle(LaviActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    public String start(LaviCommandSpec commandSpec) {
        Map<String, Object> action = actionRegistry.createAction(
                commandSpec.getActionType(),
                commandSpec.getCommand(),
                commandSpec.getRequest()
        );
        String actionId = (String) action.get("action_id");
        actionRegistry.markRunning(actionId);
        return actionId;
    }

    public void succeeded(String actionId, String message) {
        actionRegistry.markSucceeded(actionId, message);
    }

    public void failed(String actionId, String error) {
        actionRegistry.markFailed(actionId, error);
    }

    public Map<String, Object> currentSnapshot() {
        return actionRegistry.currentActionSnapshotOnly();
    }
}
