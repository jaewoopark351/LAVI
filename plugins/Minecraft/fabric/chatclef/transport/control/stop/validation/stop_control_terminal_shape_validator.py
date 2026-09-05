#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping


class StopControlTerminalShapeValidator:
    def __init__(self, *, target_validator: object, tick_validator: object):
        self._target_validator = target_validator
        self._tick_validator = tick_validator

    def valid_raw_result_shape(
        self,
        payload: dict[str, object],
        result: object,
    ) -> bool:
        return (
            type(payload.get("request_id")) is str
            and type(payload.get("ok")) is bool
            and type(payload.get("status")) is str
            and (
                payload.get("error_code") is None
                or type(payload.get("error_code")) is str
            )
            and type(payload.get("message")) is str
            and getattr(result, "request_id", None) == payload["request_id"]
            and getattr(result, "ok", None) is payload["ok"]
            and self._enum_value(getattr(result, "status", None))
            == payload["status"]
            and self._enum_value(getattr(result, "error_code", None))
            == payload["error_code"]
        )

    def valid_common_authority_types(
        self,
        data: Mapping[str, object],
    ) -> bool:
        return (
            type(data.get("control_outcome")) is str
            and type(data.get("control_reason")) is str
            and (
                data.get("target_scope") is None
                or type(data.get("target_scope")) is str
            )
            and type(data.get("target_resolution")) is str
            and type(data.get("target_state_before")) is str
            and type(data.get("target_state_after")) is str
            and type(data.get("original_result_delivery")) is str
            and type(data.get("stop_command_invoked")) is bool
            and type(data.get("java_socket_generation")) is int
            and data.get("java_socket_generation") > 0
            and self._tick_validator.nullable(data.get("executed_client_tick"))
            and self._tick_validator.nullable(data.get("verified_client_tick"))
            and self._target_validator.identity_tuple_shape(
                self._target_validator.target_tuple(
                    data,
                    "requested_target_",
                )
            )
            and self._target_validator.identity_tuple_shape(
                self._target_validator.target_tuple(
                    data,
                    "resolved_target_",
                )
            )
        )

    def _enum_value(self, value: object) -> object:
        if value is None:
            return None
        return getattr(value, "value", value)


__all__ = ("StopControlTerminalShapeValidator",)
