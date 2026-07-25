package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this factory to keep HTTP error payload shape consistent.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviHttpErrorFactory {

    public Map<String, Object> error(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", code);
        result.put("message", message == null ? "" : message);
        return result;
    }
}
