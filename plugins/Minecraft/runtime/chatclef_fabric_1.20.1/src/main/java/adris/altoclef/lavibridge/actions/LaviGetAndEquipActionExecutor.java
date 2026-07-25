package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this executor for collect-then-select Minecraft actions.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.Map;

public class LaviGetAndEquipActionExecutor {

    private final MinecraftThreadDispatcher dispatcher;
    private final LaviAcceptedResponseFactory responseFactory;
    private final LaviMinecraftActionGuard actionGuard;
    private final LaviCommandActionLifecycle actionLifecycle;
    private final LaviGetAndEquipCommandRunner commandRunner;

    public LaviGetAndEquipActionExecutor(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry
    ) {
        this.dispatcher = dispatcher;
        this.responseFactory = new LaviAcceptedResponseFactory();
        this.actionGuard = new LaviMinecraftActionGuard(mod, actionRegistry);
        this.actionLifecycle = new LaviCommandActionLifecycle(actionRegistry);
        this.commandRunner = new LaviGetAndEquipCommandRunner(mod);
    }

    public Map<String, Object> execute(LaviCommandSpec commandSpec) throws Exception {
        return dispatcher.call(() -> {
            actionGuard.ensureInGame();
            actionGuard.ensureNoRunningAction();
            actionGuard.ensureNoManualTask();

            String actionId = actionLifecycle.start(commandSpec);
            commandRunner.run(commandSpec, actionId, actionLifecycle);
            return responseFactory.accepted(actionLifecycle.currentSnapshot());
        });
    }
}
