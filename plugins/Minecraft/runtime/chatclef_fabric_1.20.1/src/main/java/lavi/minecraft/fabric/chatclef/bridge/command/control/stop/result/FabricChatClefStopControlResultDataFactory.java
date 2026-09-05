package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Own canonical STOP result field projection and immutability.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricChatClefStopControlResultDataFactory {
    public Map<String, Object> baseData(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefStopControlTargetScope scope,
            FabricChatClefStopControlRequest request
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("request_kind", "stop_control_v1");
        data.put("operation", "stop_ai");
        data.put("target_scope", scope == null ? null : scope.wireValue());
        boolean echoTrackedTarget = request != null
                && scope == FabricChatClefStopControlTargetScope.TRACKED_COMMAND;
        data.put("requested_target_request_id", echoTrackedTarget ? request.targetRequestId() : null);
        data.put("requested_target_command_message_id", echoTrackedTarget ? request.targetCommandMessageId() : null);
        data.put("requested_target_session_id", echoTrackedTarget ? request.targetSessionId() : null);
        data.put(
                "requested_target_server_connection_generation",
                echoTrackedTarget ? request.targetServerConnectionGeneration() : null
        );
        data.put("connection_generation", base.identity().serverConnectionGeneration());
        data.put("java_socket_generation", base.javaSocketGeneration());
        return data;
    }

    public void putResolvedTarget(Map<String, Object> data, FabricChatClefCommandContext context) {
        data.put("resolved_target_request_id", context == null ? null : context.requestId());
        data.put("resolved_target_command_message_id", context == null ? null : context.correlationId());
        data.put("resolved_target_session_id", context == null ? null : context.sessionId());
        data.put(
                "resolved_target_server_connection_generation",
                context == null ? null : context.serverConnectionGeneration()
        );
    }

    public Map<String, Object> immutable(Map<String, Object> data) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
