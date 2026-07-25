package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this validator to keep get-item input rules separate from command text creation.

import java.util.Map;
import java.util.regex.Pattern;

public class LaviGetItemRequestValidator {

    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("[a-z0-9_:.\\-]+");
    private static final int MAX_ITEM_COUNT = 4096;

    public LaviGetItemRequest validate(Map<String, Object> request) {
        String item = normalizeItemName(readRequiredString(request, "item"));
        int count = readCount(request);
        return new LaviGetItemRequest(item, count, request);
    }

    private static String readRequiredString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required string field: " + key);
        }
        return text.trim();
    }

    private static int readCount(Map<String, Object> request) {
        Object value = request.getOrDefault("count", 1);
        int count;
        if (value instanceof Number number) {
            count = number.intValue();
        } else if (value instanceof String text) {
            count = Integer.parseInt(text);
        } else {
            throw new IllegalArgumentException("count must be a number.");
        }

        if (count < 1 || count > MAX_ITEM_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_ITEM_COUNT + ".");
        }
        return count;
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
