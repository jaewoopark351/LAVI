package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added snapshot factory to isolate action JSON shape.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviActionSnapshotFactory {

    public Map<String, Object> toMap(LaviActionRecord action) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action_id", action.getActionId());
        result.put("type", action.getType());
        result.put("status", action.getStatus().name().toLowerCase());
        result.put("command", action.getCommand());
        result.put("request", action.getRequest());
        result.put("message", action.getMessage());
        result.put("error", action.getError());
        result.put("created_at", action.getCreatedAt().toString());
        result.put("updated_at", action.getUpdatedAt().toString());
        return result;
    }
}
