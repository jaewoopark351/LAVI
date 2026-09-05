package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics;

//20260905_kpopmodder: Own the canonical STOP transition field schema and order.

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FabricChatClefStopControlTransitionSchema {
    private static final List<String> FIELD_ORDER = List.of(
            "event",
            "control_request_id",
            "control_message_id",
            "session_id",
            "server_connection_generation",
            "accepted_session_id_at_validation",
            "accepted_server_connection_generation_at_validation",
            "java_socket_generation",
            "target_scope",
            "target_resolution",
            "requested_target_request_id",
            "requested_target_command_message_id",
            "requested_target_session_id",
            "requested_target_server_connection_generation",
            "invalid_target_fields_mask",
            "resolved_target_request_id",
            "resolved_target_command_message_id",
            "resolved_target_session_id",
            "resolved_target_server_connection_generation",
            "control_status",
            "control_outcome",
            "target_state_before",
            "target_state_after",
            "control_reason",
            "diagnostic_disposition",
            "executed_client_tick",
            "verified_client_tick",
            "stop_command_invoked",
            "original_result_delivery",
            "control_result_delivery",
            "python_stop_barrier_state",
            "java_stop_barrier_state",
            "ordinary_owner_gate_state",
            "frozen_python_owner_exact_match",
            "quarantine_active",
            "retirement_evidence_kind"
    );

    public List<String> fieldOrder() {
        return FIELD_ORDER;
    }

    public Map<String, Object> emptyFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String field : FIELD_ORDER) {
            fields.put(field, null);
        }
        fields.put("event", "stop_control_transition");
        return fields;
    }
}
