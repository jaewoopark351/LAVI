package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Build v1 command result payloads for the Fabric ChatClef bridge.
public final class FabricChatClefCommandResult {
    private FabricChatClefCommandResult() {
    }

    public static Map<String, Object> completed(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.COMPLETED, null, message);
    }

    public static Map<String, Object> completed(String requestId, String message, Map<String, Object> data) {
        return result(requestId, FabricChatClefCommandResultStatus.COMPLETED, null, message, data);
    }

    public static Map<String, Object> running(String requestId, String message, Map<String, Object> data) {
        return result(requestId, FabricChatClefCommandResultStatus.RUNNING, null, message, data);
    }

    public static Map<String, Object> unknown(String requestId, String message, Map<String, Object> data) {
        return result(requestId, FabricChatClefCommandResultStatus.UNKNOWN, null, message, data);
    }

    public static Map<String, Object> failed(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.FAILED, "internal_error", message);
    }

    public static Map<String, Object> failed(String requestId, String message, Map<String, Object> data) {
        return result(requestId, FabricChatClefCommandResultStatus.FAILED, "internal_error", message, data);
    }

    public static Map<String, Object> rejected(String requestId, String errorCode, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.REJECTED, errorCode, message);
    }

    public static Map<String, Object> deadlineExceeded(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.DEADLINE_EXCEEDED, "deadline_exceeded", message);
    }

    public static Map<String, Object> deadlineExceeded(String requestId, String message, Map<String, Object> data) {
        return result(requestId, FabricChatClefCommandResultStatus.DEADLINE_EXCEEDED, "deadline_exceeded", message, data);
    }

    private static Map<String, Object> result(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message
    ) {
        return result(requestId, status, errorCode, message, new HashMap<String, Object>());
    }

    private static Map<String, Object> result(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        return FabricChatClefCommandResultPayload.of(requestId, status, errorCode, message, data).toMap();
    }
}
