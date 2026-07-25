package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this builder to expose LAVI craft as AltoClef's get-backed crafting command.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviCraftCommandBuilder {

    public LaviCommandSpec build(LaviGetItemRequest request) {
        return new LaviCommandSpec(
                "craft",
                "get " + request.getItem() + " " + request.getCount(),
                normalizedRequest(request)
        );
    }

    private Map<String, Object> normalizedRequest(LaviGetItemRequest request) {
        Map<String, Object> normalized = new LinkedHashMap<>(request.getOriginalRequest());
        normalized.put("item", request.getItem());
        normalized.put("count", request.getCount());
        return normalized;
    }
}
