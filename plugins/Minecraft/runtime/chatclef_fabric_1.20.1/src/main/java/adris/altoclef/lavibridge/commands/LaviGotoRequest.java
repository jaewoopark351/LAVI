package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this value object to keep goto validation output explicit.

import java.util.Map;

public class LaviGotoRequest {

    private final String target;
    private final Map<String, Object> originalRequest;

    public LaviGotoRequest(String target, Map<String, Object> originalRequest) {
        this.target = target;
        this.originalRequest = originalRequest;
    }

    public String getTarget() {
        return target;
    }

    public Map<String, Object> getOriginalRequest() {
        return originalRequest;
    }
}
