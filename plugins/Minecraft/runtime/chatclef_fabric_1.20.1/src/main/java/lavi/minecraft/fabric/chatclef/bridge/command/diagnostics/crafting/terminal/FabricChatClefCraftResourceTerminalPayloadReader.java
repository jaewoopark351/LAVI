package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Read only fields already produced by existing lifecycle diagnostics.
final class FabricChatClefCraftResourceTerminalPayloadReader {
    private FabricChatClefCraftResourceTerminalPayloadReader() {
    }

    static Optional<String> string(Map<String, Object> fields, String key) {
        Object value = fields == null ? null : fields.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(stringValue);
    }

    static Optional<Boolean> bool(Map<String, Object> fields, String key) {
        Object value = fields == null ? null : fields.get(key);
        return value instanceof Boolean booleanValue
                ? Optional.of(booleanValue)
                : Optional.empty();
    }

    static Map<String, Object> nested(Map<String, Object> fields, String key) {
        Object value = fields == null ? null : fields.get(key);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                return Map.of();
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) rawMap;
        return typed;
    }
}
