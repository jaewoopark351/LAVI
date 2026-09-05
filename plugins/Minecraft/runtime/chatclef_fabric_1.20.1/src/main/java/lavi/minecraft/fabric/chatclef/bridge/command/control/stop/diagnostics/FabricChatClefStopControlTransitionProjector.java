package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics;

//20260905_kpopmodder: Project STOP lifecycle evidence into the canonical transition schema.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.Map;

public final class FabricChatClefStopControlTransitionProjector {
    private final FabricChatClefStopControlTransitionSchema schema;

    public FabricChatClefStopControlTransitionProjector(FabricChatClefStopControlTransitionSchema schema) {
        this.schema = schema;
    }

    public Map<String, Object> wireResult(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefCommandResultPayload payload,
            String invalidTargetFieldsMask,
            String controlResultDelivery,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive
    ) {
        Map<String, Object> payloadMap = payload == null ? Map.of() : payload.toMap();
        Map<String, Object> data = data(payloadMap);
        Map<String, Object> fields = baseFields(
                base,
                base == null ? null : base.javaSocketGeneration(),
                base == null ? null : base.acceptedSessionIdAtValidation(),
                base == null ? null : base.acceptedServerConnectionGenerationAtValidation()
        );
        copy(fields, data, "target_scope");
        copy(fields, data, "target_resolution");
        copyRequestedTarget(fields, data);
        fields.put("invalid_target_fields_mask", noneIfBlank(invalidTargetFieldsMask));
        copyResolvedTarget(fields, data);
        fields.put("control_status", payloadMap.get("status"));
        copy(fields, data, "control_outcome");
        copy(fields, data, "target_state_before");
        copy(fields, data, "target_state_after");
        copy(fields, data, "control_reason");
        fields.put("diagnostic_disposition", null);
        copy(fields, data, "executed_client_tick");
        copy(fields, data, "verified_client_tick");
        copy(fields, data, "stop_command_invoked");
        copy(fields, data, "original_result_delivery");
        fields.put("control_result_delivery", noneIfBlank(controlResultDelivery));
        fields.put("python_stop_barrier_state", "unknown");
        fields.put("java_stop_barrier_state", noneIfBlank(javaStopBarrierState));
        fields.put("ordinary_owner_gate_state", noneIfBlank(ordinaryOwnerGateState));
        fields.put("frozen_python_owner_exact_match", frozenPythonMatch(data.get("target_scope")));
        fields.put("quarantine_active", quarantineActive);
        fields.put("retirement_evidence_kind", retirementEvidence(data));
        return fields;
    }

    public Map<String, Object> noWire(
            FabricChatClefStopControlBaseRequest base,
            long javaSocketGeneration,
            String acceptedSessionId,
            Long acceptedServerConnectionGeneration,
            String invalidTargetFieldsMask,
            String diagnosticDisposition,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive
    ) {
        Map<String, Object> fields = baseFields(
                base,
                javaSocketGeneration,
                acceptedSessionId,
                acceptedServerConnectionGeneration
        );
        fields.put("invalid_target_fields_mask", noneIfBlank(invalidTargetFieldsMask));
        fields.put("diagnostic_disposition", noneIfBlank(diagnosticDisposition));
        fields.put("control_result_delivery", "not_applicable");
        fields.put("python_stop_barrier_state", "unknown");
        fields.put("java_stop_barrier_state", noneIfBlank(javaStopBarrierState));
        fields.put("ordinary_owner_gate_state", noneIfBlank(ordinaryOwnerGateState));
        fields.put("frozen_python_owner_exact_match", "unknown");
        fields.put("quarantine_active", quarantineActive);
        fields.put("retirement_evidence_kind", "none");
        return fields;
    }

    public Map<String, Object> contextBoundary(
            FabricChatClefStopControlContext context,
            String diagnosticDisposition,
            String javaStopBarrierState,
            String ordinaryOwnerGateState,
            boolean quarantineActive,
            String retirementEvidenceKind
    ) {
        FabricChatClefStopControlRequest request = context == null ? null : context.request();
        FabricChatClefStopControlBaseRequest base = request == null ? null : request.base();
        Map<String, Object> fields = baseFields(
                base,
                base == null ? null : base.javaSocketGeneration(),
                base == null ? null : base.acceptedSessionIdAtValidation(),
                base == null ? null : base.acceptedServerConnectionGenerationAtValidation()
        );
        if (request != null) {
            fields.put("target_scope", request.targetScope().wireValue());
            putRequestedTarget(fields, request);
        }
        FabricChatClefOrdinaryCommandStopCapture capture = context == null ? null : context.capture();
        boolean mutationPossible = context != null && context.stopCommandInvoked();
        if (capture == null) {
            fields.put("target_resolution", "not_evaluated");
            fields.put("target_state_before", "not_evaluated");
            fields.put("target_state_after", mutationPossible || quarantineActive ? "unknown" : "unchanged");
            fields.put("original_result_delivery", "not_applicable");
        } else {
            fields.put("target_resolution", context.targetResolution());
            fields.put("target_state_before", capture.state());
            fields.put("target_state_after", mutationPossible || quarantineActive ? "unknown" : "unchanged");
            putResolvedTarget(fields, capture.context());
            fields.put(
                    "original_result_delivery",
                    capture.absent() ? "not_applicable" : originalDelivery(capture.context())
            );
        }
        fields.put("invalid_target_fields_mask", "none");
        fields.put("diagnostic_disposition", noneIfBlank(diagnosticDisposition));
        long executed = context == null ? -1L : context.executedClientTick();
        fields.put("executed_client_tick", executed < 0L ? null : executed);
        fields.put("verified_client_tick", null);
        fields.put("stop_command_invoked", context != null && context.stopCommandInvoked());
        fields.put("control_result_delivery", "not_applicable");
        fields.put("python_stop_barrier_state", "unknown");
        fields.put("java_stop_barrier_state", noneIfBlank(javaStopBarrierState));
        fields.put("ordinary_owner_gate_state", noneIfBlank(ordinaryOwnerGateState));
        fields.put(
                "frozen_python_owner_exact_match",
                request != null && request.targetScope() == FabricChatClefStopControlTargetScope.TRACKED_COMMAND
                        ? "unknown"
                        : "not_applicable"
        );
        fields.put("quarantine_active", quarantineActive);
        fields.put("retirement_evidence_kind", noneIfBlank(retirementEvidenceKind));
        return fields;
    }

    private Map<String, Object> baseFields(
            FabricChatClefStopControlBaseRequest base,
            Long javaSocketGeneration,
            String acceptedSessionId,
            Long acceptedServerConnectionGeneration
    ) {
        Map<String, Object> fields = schema.emptyFields();
        if (base != null && base.identity() != null) {
            fields.put("control_request_id", base.identity().requestId());
            fields.put("control_message_id", base.identity().messageId());
            fields.put("session_id", base.identity().sessionId());
            fields.put("server_connection_generation", base.identity().serverConnectionGeneration());
        }
        fields.put("accepted_session_id_at_validation", acceptedSessionId);
        fields.put("accepted_server_connection_generation_at_validation", acceptedServerConnectionGeneration);
        fields.put("java_socket_generation", javaSocketGeneration);
        return fields;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> payload) {
        Object value = payload.get("data");
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private void copy(Map<String, Object> target, Map<String, Object> source, String key) {
        target.put(key, source.get(key));
    }

    private void copyRequestedTarget(Map<String, Object> target, Map<String, Object> source) {
        copy(target, source, "requested_target_request_id");
        copy(target, source, "requested_target_command_message_id");
        copy(target, source, "requested_target_session_id");
        copy(target, source, "requested_target_server_connection_generation");
    }

    private void copyResolvedTarget(Map<String, Object> target, Map<String, Object> source) {
        copy(target, source, "resolved_target_request_id");
        copy(target, source, "resolved_target_command_message_id");
        copy(target, source, "resolved_target_session_id");
        copy(target, source, "resolved_target_server_connection_generation");
    }

    private void putRequestedTarget(Map<String, Object> fields, FabricChatClefStopControlRequest request) {
        if (request.targetScope() != FabricChatClefStopControlTargetScope.TRACKED_COMMAND) {
            return;
        }
        fields.put("requested_target_request_id", request.targetRequestId());
        fields.put("requested_target_command_message_id", request.targetCommandMessageId());
        fields.put("requested_target_session_id", request.targetSessionId());
        fields.put("requested_target_server_connection_generation", request.targetServerConnectionGeneration());
    }

    private void putResolvedTarget(Map<String, Object> fields, FabricChatClefCommandContext context) {
        if (context == null) {
            return;
        }
        fields.put("resolved_target_request_id", context.requestId());
        fields.put("resolved_target_command_message_id", context.correlationId());
        fields.put("resolved_target_session_id", context.sessionId());
        fields.put("resolved_target_server_connection_generation", context.serverConnectionGeneration());
    }

    private String originalDelivery(FabricChatClefCommandContext context) {
        if (context == null) {
            return "not_applicable";
        }
        if (context.terminalSent()) {
            return "sent";
        }
        if (context.terminalSendRetryExhausted()) {
            return "failed";
        }
        return "unknown";
    }

    private String frozenPythonMatch(Object targetScope) {
        return "tracked_command".equals(targetScope) ? "unknown" : "not_applicable";
    }

    private String retirementEvidence(Map<String, Object> data) {
        String reason = string(data.get("control_reason"));
        if (reason == null) {
            return "none";
        }
        if (reason.endsWith("_stopped")) {
            return "original_result_sent_and_context_retired";
        }
        if ("tracked_target_absent_global_stop_executed".equals(reason)
                || "global_stop_executed_no_lavi_context".equals(reason)) {
            return "synchronous_registered_stop_completion";
        }
        if ("target_observation_failed".equals(reason)) {
            return "target_observation_uncertain";
        }
        if ("user_stop_marker_bind_failed".equals(reason)) {
            return "marker_bind_uncertain";
        }
        if ("stop_command_exception".equals(reason)) {
            return "registered_stop_exception";
        }
        if ("original_cancel_send_failed".equals(reason)) {
            return "original_result_delivery_failed";
        }
        if ("verification_timeout".equals(reason)) {
            return "retirement_verification_timeout";
        }
        return "proved_no_mutation";
    }

    private String noneIfBlank(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private String string(Object value) {
        return value instanceof String text ? text : null;
    }
}
