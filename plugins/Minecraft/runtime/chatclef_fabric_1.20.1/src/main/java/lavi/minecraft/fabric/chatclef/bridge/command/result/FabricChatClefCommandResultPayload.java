package lavi.minecraft.fabric.chatclef.bridge.command.result;

import java.util.HashMap;
import java.util.Map;

//20260804_kpopmodder: Keep command result payload fields typed until the v1 Map serialization edge.
public final class FabricChatClefCommandResultPayload {
    private final String requestId;
    private final FabricChatClefCommandResultStatus status;
    private final String errorCode;
    private final String message;
    private final Map<String, Object> data;

    private FabricChatClefCommandResultPayload(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.requestId = requestId == null ? "" : requestId;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message == null ? "" : message;
        this.data = data == null ? new HashMap<>() : data;
    }

    public static FabricChatClefCommandResultPayload of(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        return new FabricChatClefCommandResultPayload(requestId, status, errorCode, message, data);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("request_id", requestId);
        payload.put("ok", status.ok());
        payload.put("status", status.wireValue());
        payload.put("error_code", errorCode);
        payload.put("message", message);
        payload.put("data", data);
        return payload;
    }
}
