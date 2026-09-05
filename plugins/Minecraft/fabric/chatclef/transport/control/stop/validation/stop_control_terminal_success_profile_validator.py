#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping

from .stop_control_terminal_profile_schema import STOP_CONTROL_SUCCESS_PROFILES


class StopControlTerminalSuccessProfileValidator:
    PROFILES = STOP_CONTROL_SUCCESS_PROFILES

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
        expected = self.PROFILES[reason]
        if not (
            payload.get("status") == "completed"
            and payload.get("ok") is True
            and payload.get("error_code") is None
            and data.get("control_outcome") == "stopped"
            and data.get("stop_command_invoked") is True
            and (
                data.get("target_scope"),
                data.get("target_resolution"),
                data.get("target_state_before"),
                data.get("target_state_after"),
                data.get("original_result_delivery"),
            )
            == expected
            and self._target_validator.valid_declared_target(data, tracker)
            and self._target_validator.valid_resolved_target(data, tracker)
        ):
            return False
        executed = data.get("executed_client_tick")
        verified = data.get("verified_client_tick")
        if not self._tick_validator.ordered(executed, verified):
            return False
        if data.get("target_resolution") == "none":
            return verified == executed
        return True


__all__ = ("StopControlTerminalSuccessProfileValidator",)
