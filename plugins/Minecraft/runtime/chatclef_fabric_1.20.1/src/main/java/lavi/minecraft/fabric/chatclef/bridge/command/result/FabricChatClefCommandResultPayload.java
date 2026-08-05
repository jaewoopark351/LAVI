package lavi.minecraft.fabric.chatclef.bridge.command.result;

import java.util.Map;

//20260804_kpopmodder: Keep command result payload fields typed until the v1 Map serialization edge.
public final class FabricChatClefCommandResultPayload {
    private final String requestId;
    private final FabricChatClefCommandResultStatus status;
    private final String errorCode;
    private final String message;
    private final FabricChatClefCommandResultDataPayload data;

    private FabricChatClefCommandResultPayload(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            FabricChatClefCommandResultDataPayload data
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.requestId = requestId == null ? "" : requestId;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message == null ? "" : message;
        this.data = data == null ? FabricChatClefCommandResultDataPayload.empty() : data;
    }

    public static FabricChatClefCommandResultPayload of(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        return of(requestId, status, errorCode, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload of(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            FabricChatClefCommandResultDataPayload data
    ) {
        return new FabricChatClefCommandResultPayload(requestId, status, errorCode, message, data);
    }

    public Map<String, Object> toMap() {
        return FabricChatClefCommandResultPayloadMap.toMap(this);
    }

    String requestId() {
        return requestId;
    }

    FabricChatClefCommandResultStatus status() {
        return status;
    }

    String errorCode() {
        return errorCode;
    }

    String message() {
        return message;
    }

    Map<String, Object> data() {
        return data.toMap();
    }
}
