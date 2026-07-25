package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this adapter to translate LAVI actions into existing AltoClef commands.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.actions.LaviStopActionExecutor;
import adris.altoclef.lavibridge.actions.LaviUserCommandExecutor;
import adris.altoclef.lavibridge.commands.LaviCommandRequestFactory;
import java.util.Map;

public class LaviCommandAdapter {

    private final LaviCommandRequestFactory commandFactory;
    private final LaviUserCommandExecutor commandExecutor;
    private final LaviStopActionExecutor stopExecutor;

    public LaviCommandAdapter(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry,
            LaviStopController stopController
    ) {
        this.commandFactory = new LaviCommandRequestFactory();
        this.commandExecutor = new LaviUserCommandExecutor(mod, dispatcher, actionRegistry);
        this.stopExecutor = new LaviStopActionExecutor(mod, dispatcher, actionRegistry, stopController);
    }

    public Map<String, Object> getItem(Map<String, Object> request) throws Exception {
        return commandExecutor.execute(commandFactory.getItem(request));
    }

    public Map<String, Object> gotoTarget(Map<String, Object> request) throws Exception {
        return commandExecutor.execute(commandFactory.gotoTarget(request));
    }

    public Map<String, Object> stop() throws Exception {
        return stopExecutor.stop();
    }
}
