package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

//20260901_kpopmodder: Separate immutable terminal payload data from physical emission.
public record FabricChatClefCraftResourceTerminalPayload(
        Map<String, Object> requiredFields,
        Map<String, Object> optionalFields) {
    public FabricChatClefCraftResourceTerminalPayload {
        requiredFields = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(
                requiredFields,
                "requiredFields"
        )));
        optionalFields = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(
                optionalFields,
                "optionalFields"
        )));
    }

    public Object[] requiredFieldArray() {
        return fieldArray(requiredFields);
    }

    public Object[] optionalFieldArray() {
        return fieldArray(optionalFields);
    }

    private static Object[] fieldArray(Map<String, Object> fields) {
        Object[] result = new Object[fields.size() * 2];
        int index = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            result[index++] = entry.getKey();
            result[index++] = entry.getValue();
        }
        return result;
    }
}
