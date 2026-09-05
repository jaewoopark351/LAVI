#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_terminal_decision import (
    StopControlTerminalDecision,
)

from .stop_control_terminal_profile_schema import (
    STOP_CONTROL_NO_MUTATION_REASONS,
    STOP_CONTROL_REQUIRED_DATA_FIELDS,
    STOP_CONTROL_REQUIRED_RESULT_FIELDS,
    STOP_CONTROL_SUCCESS_PROFILES,
    STOP_CONTROL_UNKNOWN_REASONS,
)


class StopControlTerminalValidationCoordinator:
    def __init__(
        self,
        *,
        shape_validator: object,
        identity_validator: object,
        success_validator: object,
        no_mutation_validator: object,
        uncertainty_validator: object,
    ):
        self._shape_validator = shape_validator
        self._identity_validator = identity_validator
        self._success_validator = success_validator
        self._no_mutation_validator = no_mutation_validator
        self._uncertainty_validator = uncertainty_validator

    def inspect(
        self,
        envelope: object,
        result: object,
        tracker: object,
    ) -> StopControlTerminalDecision:
        payload = getattr(envelope, "payload", None)
        if type(payload) is not dict:
            return self._unknown("missing_control_payload")
        data = payload.get("data")
        if type(data) is not dict:
            return self._unknown("missing_control_data")
        if set(payload) != STOP_CONTROL_REQUIRED_RESULT_FIELDS:
            return self._unknown("unknown_control_result_field")
        data_fields = set(data)
        if not STOP_CONTROL_REQUIRED_DATA_FIELDS.issubset(data_fields):
            return self._unknown("incomplete_control_result_profile")
        if data_fields != STOP_CONTROL_REQUIRED_DATA_FIELDS:
            return self._unknown("unknown_control_authorization_field")
        if not self._shape_validator.valid_raw_result_shape(payload, result):
            return self._unknown("invalid_control_result_shape")
        if not self._identity_validator.valid(
            envelope=envelope,
            payload=payload,
            data=data,
            result=result,
            tracker=tracker,
        ):
            return self._unknown("control_result_identity_mismatch")
        if (
            data.get("request_kind") != "stop_control_v1"
            or data.get("operation") != "stop_ai"
        ):
            return self._unknown("invalid_control_result_profile")
        if not self._shape_validator.valid_common_authority_types(data):
            return self._unknown("invalid_control_result_values")

        status = payload["status"]
        outcome = data["control_outcome"]
        reason = data["control_reason"]
        if reason in STOP_CONTROL_SUCCESS_PROFILES:
            valid = self._success_validator.valid(
                payload=payload,
                data=data,
                tracker=tracker,
                reason=reason,
            )
            return self._decision(valid, valid, status, outcome, reason)
        if reason in STOP_CONTROL_NO_MUTATION_REASONS:
            valid = self._no_mutation_validator.valid(
                payload=payload,
                data=data,
                tracker=tracker,
                reason=reason,
            )
            return self._decision(valid, valid, status, outcome, reason)
        if reason in STOP_CONTROL_UNKNOWN_REASONS:
            valid = self._uncertainty_validator.valid(
                payload=payload,
                data=data,
                tracker=tracker,
                reason=reason,
            )
            return self._decision(valid, False, status, outcome, reason)
        return self._unknown("unknown_control_reason")

    def _decision(
        self,
        valid: bool,
        release_barrier: bool,
        status: str,
        outcome: str,
        reason: str,
    ) -> StopControlTerminalDecision:
        if not valid:
            return self._unknown("contradictory_control_result_profile")
        return StopControlTerminalDecision(
            valid=True,
            release_barrier=release_barrier,
            status=status,
            control_outcome=outcome,
            reason=reason,
        )

    def _unknown(self, reason: str) -> StopControlTerminalDecision:
        return StopControlTerminalDecision(
            valid=False,
            release_barrier=False,
            status="unknown",
            control_outcome="unknown",
            reason=str(reason or "unknown"),
        )


__all__ = ("StopControlTerminalValidationCoordinator",)
