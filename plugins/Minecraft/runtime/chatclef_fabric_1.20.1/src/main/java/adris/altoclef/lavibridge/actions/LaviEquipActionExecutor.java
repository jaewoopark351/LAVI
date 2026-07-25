package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this executor so LAVI equip means selecting an inventory item in hand.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.Map;

public class LaviEquipActionExecutor {

    private final MinecraftThreadDispatcher dispatcher;
    private final LaviAcceptedResponseFactory responseFactory;
    private final LaviMinecraftActionGuard actionGuard;
    private final LaviCommandActionLifecycle actionLifecycle;
    private final LaviEquipItemRunner equipItemRunner;

    public LaviEquipActionExecutor(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry
    ) {
        this.dispatcher = dispatcher;
        this.responseFactory = new LaviAcceptedResponseFactory();
        this.actionGuard = new LaviMinecraftActionGuard(mod, actionRegistry);
        this.actionLifecycle = new LaviCommandActionLifecycle(actionRegistry);
        this.equipItemRunner = new LaviEquipItemRunner(mod);
    }

    public Map<String, Object> execute(LaviCommandSpec commandSpec) throws Exception {
        return dispatcher.call(() -> {
            actionGuard.ensureInGame();
            actionGuard.ensureNoRunningAction();
            actionGuard.ensureNoManualTask();

            String actionId = actionLifecycle.start(commandSpec);
            try {
                actionLifecycle.succeeded(actionId, equipItemRunner.equip(commandSpec));
            } catch (RuntimeException exception) {
                actionLifecycle.failed(actionId, exception.getMessage());
            }
            return responseFactory.accepted(actionLifecycle.currentSnapshot());
        });
    }
}
