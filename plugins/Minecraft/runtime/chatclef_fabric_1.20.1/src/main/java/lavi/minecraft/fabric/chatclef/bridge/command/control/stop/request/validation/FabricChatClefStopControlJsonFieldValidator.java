package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Centralize strict JSON scalar and exact-field validation for STOP requests.

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public final class FabricChatClefStopControlJsonFieldValidator {
    private static final int MAX_ID_LENGTH = 512;

    public boolean hasOnlyFields(JsonNode object, Set<String> allowedFields) {
        if (object == null || !object.isObject() || object.size() != allowedFields.size()) {
            return false;
        }
        for (String field : allowedFields) {
            if (!object.has(field)) {
                return false;
            }
        }
        return true;
    }

    public boolean exactTextEquals(JsonNode value, String expected) {
        return value != null && value.isTextual() && expected.equals(value.textValue());
    }

    public boolean isNonBlankText(JsonNode value) {
        return exactIdentityText(value) != null;
    }

    public String exactIdentityText(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return null;
        }
        String text = value.textValue();
        return text.isBlank() || text.length() > MAX_ID_LENGTH ? null : text;
    }

    public boolean exactLongEquals(JsonNode value, long expected) {
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() == expected;
    }

    public boolean isNonNegativeLong(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0L;
    }

    public Long exactPositiveLong(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            return null;
        }
        long number = value.longValue();
        return number > 0L ? number : null;
    }
}
