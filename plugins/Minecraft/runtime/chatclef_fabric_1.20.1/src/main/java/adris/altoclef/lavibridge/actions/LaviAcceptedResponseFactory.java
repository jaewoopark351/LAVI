package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this factory to keep accepted HTTP action responses consistent.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviAcceptedResponseFactory {

    public Map<String, Object> accepted(Map<String, Object> action) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("accepted", true);
        result.put("action", action);
        return result;
    }
}
