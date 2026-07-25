package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this class to isolate AltoClef command execution and LAVI action state updates.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class LaviUserCommandExecutor {

    private final AltoClef mod;
    private final MinecraftThreadDispatcher dispatcher;
    private final LaviActionRegistry actionRegistry;
    private final LaviAcceptedResponseFactory responseFactory;
    private final LaviMinecraftActionGuard actionGuard;

    public LaviUserCommandExecutor(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry
    ) {
        this.mod = mod;
        this.dispatcher = dispatcher;
        this.actionRegistry = actionRegistry;
        this.responseFactory = new LaviAcceptedResponseFactory();
        this.actionGuard = new LaviMinecraftActionGuard(mod, actionRegistry);
    }

    public Map<String, Object> execute(LaviCommandSpec commandSpec) throws Exception {
        return dispatcher.call(() -> {
            actionGuard.ensureInGame();
            actionGuard.ensureNoRunningAction();
            actionGuard.ensureNoManualTask();

            Map<String, Object> action = actionRegistry.createAction(
                    commandSpec.getActionType(),
                    commandSpec.getCommand(),
                    commandSpec.getRequest()
            );
            String actionId = (String) action.get("action_id");
            actionRegistry.markRunning(actionId);

            AtomicBoolean failed = new AtomicBoolean(false);
            String commandLine = mod.getCommandExecutor().getCommandPrefix() + commandSpec.getCommand();

            mod.getCommandExecutor().execute(commandLine, () -> {
                if (!failed.get()) {
                    actionRegistry.markSucceeded(actionId, "AltoClef command finished.");
                }
            }, exception -> {
                failed.set(true);
                actionRegistry.markFailed(actionId, exception.getMessage());
            });

            return responseFactory.accepted(actionRegistry.currentActionSnapshotOnly());
        });
    }
}
