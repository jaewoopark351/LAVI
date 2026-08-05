package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.Map;

//20260801_kpopmodder: Build v1 command result payloads for the Fabric ChatClef bridge.
public final class FabricChatClefCommandResult {
    private FabricChatClefCommandResult() {
    }

    public static FabricChatClefCommandResultPayload completed(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.COMPLETED, null, message);
    }

    public static FabricChatClefCommandResultPayload completed(String requestId, String message, Map<String, Object> data) {
        return completed(requestId, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload completed(String requestId, String message, FabricChatClefCommandResultDataPayload data) {
        return result(requestId, FabricChatClefCommandResultStatus.COMPLETED, null, message, data);
    }

    public static FabricChatClefCommandResultPayload running(String requestId, String message, Map<String, Object> data) {
        return running(requestId, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload running(String requestId, String message, FabricChatClefCommandResultDataPayload data) {
        return result(requestId, FabricChatClefCommandResultStatus.RUNNING, null, message, data);
    }

    public static FabricChatClefCommandResultPayload unknown(String requestId, String message, Map<String, Object> data) {
        return unknown(requestId, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload unknown(String requestId, String message, FabricChatClefCommandResultDataPayload data) {
        return result(requestId, FabricChatClefCommandResultStatus.UNKNOWN, null, message, data);
    }

    public static FabricChatClefCommandResultPayload failed(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.FAILED, "internal_error", message);
    }

    public static FabricChatClefCommandResultPayload failed(String requestId, String message, Map<String, Object> data) {
        return failed(requestId, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload failed(String requestId, String message, FabricChatClefCommandResultDataPayload data) {
        return result(requestId, FabricChatClefCommandResultStatus.FAILED, "internal_error", message, data);
    }

    public static FabricChatClefCommandResultPayload rejected(String requestId, String errorCode, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.REJECTED, errorCode, message);
    }

    public static FabricChatClefCommandResultPayload deadlineExceeded(String requestId, String message) {
        return result(requestId, FabricChatClefCommandResultStatus.DEADLINE_EXCEEDED, "deadline_exceeded", message);
    }

    public static FabricChatClefCommandResultPayload deadlineExceeded(String requestId, String message, Map<String, Object> data) {
        return deadlineExceeded(requestId, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    public static FabricChatClefCommandResultPayload deadlineExceeded(String requestId, String message, FabricChatClefCommandResultDataPayload data) {
        return result(requestId, FabricChatClefCommandResultStatus.DEADLINE_EXCEEDED, "deadline_exceeded", message, data);
    }

    private static FabricChatClefCommandResultPayload result(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message
    ) {
        return result(requestId, status, errorCode, message, FabricChatClefCommandResultDataPayload.empty());
    }

    private static FabricChatClefCommandResultPayload result(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            Map<String, Object> data
    ) {
        return result(requestId, status, errorCode, message, FabricChatClefCommandResultDataPayload.fromMap(data));
    }

    private static FabricChatClefCommandResultPayload result(
            String requestId,
            FabricChatClefCommandResultStatus status,
            String errorCode,
            String message,
            FabricChatClefCommandResultDataPayload data
    ) {
        return FabricChatClefCommandResultPayload.of(requestId, status, errorCode, message, data);
    }
}
