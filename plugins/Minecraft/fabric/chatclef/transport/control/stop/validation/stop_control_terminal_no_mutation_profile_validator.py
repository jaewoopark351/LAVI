#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping

from .stop_control_terminal_profile_schema import (
    STOP_CONTROL_NO_MUTATION_REASONS,
)


class StopControlTerminalNoMutationProfileValidator:
    REASONS = STOP_CONTROL_NO_MUTATION_REASONS
    _UNTARGETED_REASONS = frozenset(
        {"invalid_control_profile", "invalid_target_scope"}
    )
    _TARGET_SCOPES = frozenset(
        {"tracked_command", "current_global_automation"}
    )

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
        deadline = reason == "deadline_exceeded"
        if not (
            payload.get("status")
            == ("deadline_exceeded" if deadline else "rejected")
            and payload.get("ok") is False
            and payload.get("error_code")
            == ("deadline_exceeded" if deadline else "invalid_request")
            and data.get("control_outcome") == "rejected"
            and data.get("target_resolution") == "not_evaluated"
            and self._target_validator.target_tuple(data, "resolved_target_")
            == self._target_validator.EMPTY_IDENTITY
            and data.get("target_state_before") == "not_evaluated"
            and data.get("target_state_after") == "not_evaluated"
            and data.get("original_result_delivery") == "not_applicable"
            and data.get("stop_command_invoked") is False
            and data.get("executed_client_tick") is None
        ):
            return False

        scope = data.get("target_scope")
        requested = self._target_validator.target_tuple(
            data,
            "requested_target_",
        )
        verified = data.get("verified_client_tick")
        if reason in self._UNTARGETED_REASONS:
            return (
                scope is None
                and requested == self._target_validator.EMPTY_IDENTITY
                and verified is None
            )
        if reason == "invalid_target_fields":
            return (
                scope == tracker.target_scope
                and scope in self._TARGET_SCOPES
                and requested == self._target_validator.EMPTY_IDENTITY
                and verified is None
            )
        if reason == "stop_control_in_flight":
            return (
                self._target_validator.valid_declared_target(data, tracker)
                and verified is None
            )
        if scope is None:
            return (
                requested == self._target_validator.EMPTY_IDENTITY
                and verified is None
            )
        return (
            self._target_validator.valid_declared_target(data, tracker)
            and self._tick_validator.exact(verified)
        )


__all__ = ("StopControlTerminalNoMutationProfileValidator",)
