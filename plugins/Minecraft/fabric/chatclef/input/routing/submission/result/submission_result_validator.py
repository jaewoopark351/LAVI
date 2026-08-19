#20260819_kpopmodder: Validate every untrusted submit-result mirror fail closed.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)

from .canonical_submission_result import CanonicalSubmissionResult
from .canonical_submission_result_factory import CanonicalSubmissionResultFactory
from .submission_outcome_consistency import SubmissionOutcomeConsistency
from .submission_payload_parser import SubmissionPayloadParser


_MISSING = object()


class SubmissionResultValidator:
    def __init__(self):
        self._payload_parser = SubmissionPayloadParser()
        self._outcome_consistency = SubmissionOutcomeConsistency()
        self._result_factory = CanonicalSubmissionResultFactory()

    def validate(
        self,
        payload: Any,
        *,
        expected_request_id: str | None = None,
    ) -> tuple[CanonicalSubmissionResult | None, str, str | None]:
        mapped = self._payload_parser.parse(payload)
        expected_id = self._request_id(expected_request_id)
        if expected_request_id is not None and expected_id is None:
            return self._invalid(
                "",
                "Fabric ChatClef expected request identity is invalid.",
            )
        if mapped is None:
            return self._invalid(
                expected_id or "",
                "Fabric ChatClef submit result is not an object.",
            )

        status_payload = self._payload_parser.mapping(mapped.get("status"))
        observed_id = (
            None
            if status_payload is None
            else self._request_id(status_payload.get("request_id"))
        )
        unknown_id = expected_id or observed_id or ""
        if status_payload is None:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result status is missing or invalid.",
            )

        outer_ok = mapped.get("ok", _MISSING)
        nested_ok = status_payload.get("ok", _MISSING)
        if type(outer_ok) is not bool or type(nested_ok) is not bool:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result success flags are missing or invalid.",
            )
        if outer_ok is not nested_ok:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result success flags are inconsistent.",
            )

        if observed_id is None:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result request identity is missing or invalid.",
            )
        if expected_id is not None and observed_id != expected_id:
            return self._invalid(
                expected_id,
                "Fabric ChatClef submit result request identity is mismatched.",
            )
        if "request_id" in mapped:
            outer_request_id = self._request_id(mapped.get("request_id"))
            if outer_request_id is None or outer_request_id != observed_id:
                return self._invalid(
                    expected_id or "",
                    "Fabric ChatClef submit result request identity mirrors disagree.",
                )

        status = self._status(status_payload.get("status", _MISSING))
        if status is None:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result status is unknown or invalid.",
            )
        if nested_ok is not status.ok:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result status and success flag are inconsistent.",
            )

        nested_error = self._error_code(
            status_payload.get("error_code", _MISSING)
        )
        outer_error = self._error_code(mapped.get("error", _MISSING))
        if nested_error is _MISSING or outer_error is _MISSING:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result error fields are missing or invalid.",
            )
        if nested_error != outer_error:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result error mirrors disagree.",
            )
        if status.ok and nested_error is not None:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef successful submit result includes an error code.",
            )

        nested_message = status_payload.get("message", _MISSING)
        outer_message = mapped.get("message", _MISSING)
        if type(nested_message) is not str or type(outer_message) is not str:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result messages are missing or invalid.",
            )
        if nested_message != outer_message:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result message mirrors disagree.",
            )

        nested_data = self._payload_parser.fresh_mapping(
            status_payload.get("data", _MISSING)
        )
        outer_details = self._payload_parser.fresh_mapping(
            mapped.get("details", _MISSING)
        )
        if nested_data is None or outer_details is None:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result details are missing or invalid.",
            )
        try:
            details_match = nested_data == outer_details
        except Exception:
            details_match = False
        if not details_match:
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result detail mirrors disagree.",
            )
        if not self._outcome_consistency.is_consistent(status, nested_data):
            return self._invalid(
                unknown_id,
                "Fabric ChatClef submit result outcome metadata is inconsistent.",
            )

        return (
            self._result_factory.create(
                request_id=observed_id,
                ok=nested_ok,
                status=status,
                error_code=nested_error,
                message=nested_message,
                data=nested_data,
            ),
            observed_id,
            None,
        )

    def _invalid(
        self,
        request_id: str,
        message: str,
    ) -> tuple[None, str, str]:
        return None, request_id, message

    def _request_id(self, value: Any) -> str | None:
        if type(value) is not str or not value or value != value.strip():
            return None
        return value

    def _status(self, value: Any) -> CommandResultStatus | None:
        if type(value) is not str:
            return None
        try:
            return CommandResultStatus(value)
        except ValueError:
            return None

    def _error_code(self, value: Any) -> str | None | object:
        if value is None:
            return None
        if type(value) is not str:
            return _MISSING
        try:
            return BridgeErrorCode(value).value
        except ValueError:
            return _MISSING
