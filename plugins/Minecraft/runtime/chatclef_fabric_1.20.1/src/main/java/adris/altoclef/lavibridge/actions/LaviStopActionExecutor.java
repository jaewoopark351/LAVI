package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this class to keep stop/cancel action state separate from command routing.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.LaviStopController;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;

import java.util.Map;

public class LaviStopActionExecutor {

    private final MinecraftThreadDispatcher dispatcher;
    private final LaviAcceptedResponseFactory responseFactory;
    private final LaviMinecraftActionGuard actionGuard;
    private final LaviRunningActionCanceller runningActionCanceller;
    private final LaviStopRequestFactory stopRequestFactory;
    private final LaviStopActionLifecycle stopActionLifecycle;
    private final LaviStopAutomationRunner stopAutomationRunner;

    public LaviStopActionExecutor(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry,
            LaviStopController stopController
    ) {
        this.dispatcher = dispatcher;
        this.responseFactory = new LaviAcceptedResponseFactory();
        this.actionGuard = new LaviMinecraftActionGuard(mod, actionRegistry);
        this.runningActionCanceller = new LaviRunningActionCanceller(actionRegistry);
        this.stopRequestFactory = new LaviStopRequestFactory();
        this.stopActionLifecycle = new LaviStopActionLifecycle(actionRegistry);
        this.stopAutomationRunner = new LaviStopAutomationRunner(stopController);
    }

    public Map<String, Object> stop() throws Exception {
        return dispatcher.call(() -> {
            actionGuard.ensureInGame();
            Map<String, Object> cancelledAction = runningActionCanceller.cancelForStopRequest();
            Map<String, Object> request = stopRequestFactory.build(cancelledAction);
            String actionId = stopActionLifecycle.start(request);
            stopAutomationRunner.stop();
            stopActionLifecycle.succeeded(actionId);
            return responseFactory.accepted(stopActionLifecycle.currentSnapshot());
        });
    }
}
