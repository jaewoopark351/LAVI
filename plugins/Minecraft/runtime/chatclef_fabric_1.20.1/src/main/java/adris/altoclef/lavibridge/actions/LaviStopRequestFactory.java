package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this factory to isolate stop request payload shape.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviStopRequestFactory {

    public Map<String, Object> build(Map<String, Object> cancelledAction) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (cancelledAction != null) {
            request.put("cancelled_action", cancelledAction);
        }
        return request;
    }
}
