package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Build v1 command result payloads for the Fabric ChatClef bridge.
public final class FabricChatClefCommandResult {
    private FabricChatClefCommandResult() {
    }

    public static Map<String, Object> completed(String requestId, String message) {
        return result(requestId, true, "completed", null, message);
    }

    public static Map<String, Object> completed(String requestId, String message, Map<String, Object> data) {
        return result(requestId, true, "completed", null, message, data);
    }

    public static Map<String, Object> running(String requestId, String message, Map<String, Object> data) {
        return result(requestId, true, "running", null, message, data);
    }

    public static Map<String, Object> unknown(String requestId, String message, Map<String, Object> data) {
        return result(requestId, false, "unknown", null, message, data);
    }

    public static Map<String, Object> failed(String requestId, String message) {
        return result(requestId, false, "failed", "internal_error", message);
    }

    public static Map<String, Object> failed(String requestId, String message, Map<String, Object> data) {
        return result(requestId, false, "failed", "internal_error", message, data);
    }

    public static Map<String, Object> rejected(String requestId, String errorCode, String message) {
        return result(requestId, false, "rejected", errorCode, message);
    }

    public static Map<String, Object> deadlineExceeded(String requestId, String message) {
        return result(requestId, false, "deadline_exceeded", "deadline_exceeded", message);
    }

    public static Map<String, Object> deadlineExceeded(String requestId, String message, Map<String, Object> data) {
        return result(requestId, false, "deadline_exceeded", "deadline_exceeded", message, data);
    }

    private static Map<String, Object> result(
            String requestId,
            boolean ok,
            String status,
            String errorCode,
            String message
    ) {
        return result(requestId, ok, status, errorCode, message, new HashMap<String, Object>());
    }

    private static Map<String, Object> result(
            String requestId,
            boolean ok,
            String status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("request_id", requestId == null ? "" : requestId);
        payload.put("ok", ok);
        payload.put("status", status);
        payload.put("error_code", errorCode);
        payload.put("message", message == null ? "" : message);
        payload.put("data", data == null ? new HashMap<String, Object>() : data);
        return payload;
    }
}
