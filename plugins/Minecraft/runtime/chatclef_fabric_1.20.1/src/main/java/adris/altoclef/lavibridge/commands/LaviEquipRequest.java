package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this value object to keep equip validation output explicit.

import java.util.Map;

public class LaviEquipRequest {

    private final String item;
    private final Map<String, Object> originalRequest;

    public LaviEquipRequest(String item, Map<String, Object> originalRequest) {
        this.item = item;
        this.originalRequest = originalRequest;
    }

    public String getItem() {
        return item;
    }

    public Map<String, Object> getOriginalRequest() {
        return originalRequest;
    }
}
