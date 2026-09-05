#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping

from .stop_control_terminal_profile_schema import STOP_CONTROL_UNKNOWN_REASONS


class StopControlTerminalUncertaintyProfileValidator:
    REASONS = STOP_CONTROL_UNKNOWN_REASONS
    _INTERNAL_ERROR_REASONS = frozenset(
        {"target_observation_failed", "stop_command_exception"}
    )
    _RESOLVED_CONTEXTS = frozenset({"exact", "captured_current"})
    _ACTIVE_STATES = frozenset({"pending", "active"})
    _TIMEOUT_DELIVERIES = frozenset({"sent", "unknown"})

    def __init__(self, *, target_validator: object, tick_validator: object):
        self._target_validator = target_validator
        self._tick_validator = tick_validator

    def valid(
        self,
        *,
        payload: Mapping[str, object],
        data: Mapping[str, object],
        tracker: object,
        reason: str,
    ) -> bool:
        expected_error = (
            "internal_error" if reason in self._INTERNAL_ERROR_REASONS else None
        )
        if not (
            payload.get("status") == "unknown"
            and payload.get("ok") is False
            and payload.get("error_code") == expected_error
            and data.get("control_outcome") == "unknown"
            and self._target_validator.valid_declared_target(data, tracker)
            and self._target_validator.valid_resolved_target(data, tracker)
            and data.get("target_state_after") == "unknown"
        ):
            return False

        resolution = data.get("target_resolution")
        state_before = data.get("target_state_before")
        delivery = data.get("original_result_delivery")
        invoked = data.get("stop_command_invoked")
        executed = data.get("executed_client_tick")
        verified = data.get("verified_client_tick")
        if reason == "target_observation_failed":
            read_failure = resolution == "unknown" and state_before == "unknown"
            reservation_failure = (
                resolution in self._RESOLVED_CONTEXTS
                and state_before == "pending"
            )
            return (
                (read_failure or reservation_failure)
                and delivery == "not_applicable"
                and invoked is False
                and executed is None
                and self._tick_validator.exact(verified)
            )
        if reason == "user_stop_marker_bind_failed":
            return (
                resolution in self._RESOLVED_CONTEXTS
                and state_before == "active"
                and delivery == "not_applicable"
                and invoked is True
                and self._tick_validator.same(executed, verified)
            )
        if reason == "stop_command_exception":
            context_shape = (
                resolution in self._RESOLVED_CONTEXTS
                and state_before in self._ACTIVE_STATES
            ) or (resolution == "none" and state_before == "none")
            return (
                context_shape
                and delivery == "not_applicable"
                and invoked is True
                and self._tick_validator.same(executed, verified)
            )
        if reason == "original_cancel_send_failed":
            return (
                resolution in self._RESOLVED_CONTEXTS
                and state_before in self._ACTIVE_STATES
                and delivery == "failed"
                and invoked is True
                and self._tick_validator.ordered(executed, verified)
            )
        return (
            resolution in self._RESOLVED_CONTEXTS
            and state_before in self._ACTIVE_STATES
            and delivery in self._TIMEOUT_DELIVERIES
            and invoked is True
            and self._tick_validator.exact_timeout_boundary(executed, verified)
        )


__all__ = ("StopControlTerminalUncertaintyProfileValidator",)
