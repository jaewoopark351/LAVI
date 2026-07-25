package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this value object to keep get-item validation output explicit.

import java.util.Map;

public class LaviGetItemRequest {

    private final String item;
    private final int count;
    private final Map<String, Object> originalRequest;

    public LaviGetItemRequest(String item, int count, Map<String, Object> originalRequest) {
        this.item = item;
        this.count = count;
        this.originalRequest = originalRequest;
    }

    public String getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public Map<String, Object> getOriginalRequest() {
        return originalRequest;
    }
}
