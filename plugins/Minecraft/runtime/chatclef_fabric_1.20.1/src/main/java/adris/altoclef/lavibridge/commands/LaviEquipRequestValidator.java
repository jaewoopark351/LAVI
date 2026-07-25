package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this validator to keep equip item input rules separate from execution.

import java.util.Map;
import java.util.regex.Pattern;

public class LaviEquipRequestValidator {

    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("[a-z0-9_:.\\-]+");

    public LaviEquipRequest validate(Map<String, Object> request) {
        String item = normalizeItemName(readRequiredString(request, "item"));
        return new LaviEquipRequest(item, request);
    }

    private static String readRequiredString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required string field: " + key);
        }
        return text.trim();
    }

    private static String normalizeItemName(String item) {
        String normalized = item.toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        if (!ITEM_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("item contains unsupported characters.");
        }
        return normalized;
    }
}
