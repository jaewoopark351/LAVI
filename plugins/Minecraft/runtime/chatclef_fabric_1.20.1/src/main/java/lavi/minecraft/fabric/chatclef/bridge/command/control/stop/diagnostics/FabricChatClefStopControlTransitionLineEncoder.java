package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics;

//20260905_kpopmodder: Encode bounded canonical STOP transition fields into one log line.

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FabricChatClefStopControlTransitionLineEncoder {
    private static final int MAX_LOG_VALUE_LENGTH = 512;
    private final FabricChatClefStopControlTransitionSchema schema;

    public FabricChatClefStopControlTransitionLineEncoder(FabricChatClefStopControlTransitionSchema schema) {
        this.schema = schema;
    }

    public String encode(Map<String, Object> fields) {
        StringBuilder line = new StringBuilder();
        for (String field : schema.fieldOrder()) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(field).append('=').append(render(fields.get(field)));
        }
        return line.toString();
    }

    private String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        return URLEncoder.encode(bounded(String.valueOf(value)), StandardCharsets.UTF_8);
    }

    private String bounded(String value) {
        if (value.length() <= MAX_LOG_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_VALUE_LENGTH - 10) + "~truncated";
    }
}
