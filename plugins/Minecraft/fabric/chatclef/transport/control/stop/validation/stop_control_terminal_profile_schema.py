#20260905_kpopmodder: Define immutable STOP terminal validation profiles.
from types import MappingProxyType


STOP_VERIFY_MAX_LATER_TICKS = 20

STOP_CONTROL_REQUIRED_RESULT_FIELDS = frozenset(
    {
        "request_id",
        "ok",
        "status",
        "error_code",
        "message",
        "data",
    }
)

STOP_CONTROL_REQUIRED_DATA_FIELDS = frozenset(
    {
        "request_kind",
        "operation",
        "control_outcome",
        "control_reason",
        "target_scope",
        "target_resolution",
        "requested_target_request_id",
        "requested_target_command_message_id",
        "requested_target_session_id",
        "requested_target_server_connection_generation",
        "resolved_target_request_id",
        "resolved_target_command_message_id",
        "resolved_target_session_id",
        "resolved_target_server_connection_generation",
        "target_state_before",
        "target_state_after",
        "original_result_delivery",
        "stop_command_invoked",
        "connection_generation",
        "java_socket_generation",
        "executed_client_tick",
        "verified_client_tick",
    }
)

STOP_CONTROL_SUCCESS_PROFILES = MappingProxyType(
    {
        "tracked_pending_stopped": (
            "tracked_command",
            "exact",
            "pending",
            "retired",
            "sent",
        ),
        "tracked_active_stopped": (
            "tracked_command",
            "exact",
            "active",
            "retired",
            "sent",
        ),
        "tracked_target_replaced_current_pending_stopped": (
            "tracked_command",
            "captured_current",
            "pending",
            "retired",
            "sent",
        ),
        "tracked_target_replaced_current_active_stopped": (
            "tracked_command",
            "captured_current",
            "active",
            "retired",
            "sent",
        ),
        "tracked_target_absent_global_stop_executed": (
            "tracked_command",
            "none",
            "none",
            "none",
            "not_applicable",
        ),
        "global_pending_stopped": (
            "current_global_automation",
            "captured_current",
            "pending",
            "retired",
            "sent",
        ),
        "global_active_stopped": (
            "current_global_automation",
            "captured_current",
            "active",
            "retired",
            "sent",
        ),
        "global_stop_executed_no_lavi_context": (
            "current_global_automation",
            "none",
            "none",
            "none",
            "not_applicable",
        ),
    }
)

STOP_CONTROL_NO_MUTATION_REASONS = frozenset(
    {
        "invalid_control_profile",
        "invalid_target_scope",
        "invalid_target_fields",
        "session_mismatch",
        "server_generation_mismatch",
        "stop_control_in_flight",
        "deadline_exceeded",
    }
)

STOP_CONTROL_UNKNOWN_REASONS = frozenset(
    {
        "target_observation_failed",
        "user_stop_marker_bind_failed",
        "stop_command_exception",
        "original_cancel_send_failed",
        "verification_timeout",
    }
)


__all__ = (
    "STOP_CONTROL_NO_MUTATION_REASONS",
    "STOP_CONTROL_REQUIRED_DATA_FIELDS",
    "STOP_CONTROL_REQUIRED_RESULT_FIELDS",
    "STOP_CONTROL_SUCCESS_PROFILES",
    "STOP_CONTROL_UNKNOWN_REASONS",
    "STOP_VERIFY_MAX_LATER_TICKS",
)
