package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this value object to separate LAVI command building from command execution.

import java.util.Map;

public class LaviCommandSpec {

    private final String actionType;
    private final String command;
    private final Map<String, Object> request;

    public LaviCommandSpec(String actionType, String command, Map<String, Object> request) {
        this.actionType = actionType;
        this.command = command;
        this.request = request;
    }

    public String getActionType() {
        return actionType;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, Object> getRequest() {
        return request;
    }
}
