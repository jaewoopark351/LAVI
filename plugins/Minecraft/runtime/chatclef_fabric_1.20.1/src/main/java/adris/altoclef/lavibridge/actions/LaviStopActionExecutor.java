package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this class to keep stop/cancel action state separate from command routing.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.LaviStopController;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviStopActionExecutor {

    private final MinecraftThreadDispatcher dispatcher;
    private final LaviActionRegistry actionRegistry;
    private final LaviStopController stopController;
    private final LaviAcceptedResponseFactory responseFactory;
    private final LaviMinecraftActionGuard actionGuard;

    public LaviStopActionExecutor(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry,
            LaviStopController stopController
    ) {
        this.dispatcher = dispatcher;
        this.actionRegistry = actionRegistry;
        this.stopController = stopController;
        this.responseFactory = new LaviAcceptedResponseFactory();
        this.actionGuard = new LaviMinecraftActionGuard(mod, actionRegistry);
    }

    public Map<String, Object> stop() throws Exception {
        return dispatcher.call(() -> {
            actionGuard.ensureInGame();
            Map<String, Object> request = new LinkedHashMap<>();
            Map<String, Object> cancelledAction = actionRegistry.cancelCurrentIfRunning("Cancelled by stop request.");
            if (cancelledAction != null) {
                request.put("cancelled_action", cancelledAction);
            }
            Map<String, Object> action = actionRegistry.createAction("stop", "stop", request);
            String actionId = (String) action.get("action_id");
            actionRegistry.markRunning(actionId);
            stopController.stopAutomation();
            actionRegistry.markSucceeded(actionId, "Stop requested.");
            return responseFactory.accepted(actionRegistry.currentActionSnapshotOnly());
        });
    }
}
