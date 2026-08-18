#20260818_kpopmodder: Normalize one untrusted extension submit result fail-closed.
#20260819_kpopmodder: Canonicalize every mirrored submit-result field before routing it.
from __future__ import annotations

import copy
from typing import Any, Mapping

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


_MISSING = object()


class MinecraftChatClefSubmissionResultNormalizer:
    SUBMISSION_OUTCOMES = {
        "accepted",
        "submit_response_not_accepted",
        "submission_outcome_unknown",
    }

    def normalize(
        self,
        payload: Any,
        *,
        expected_request_id: str | None = None,
    ) -> dict[str, Any]:
        mapped = self._mapping(payload)
        expected_id = self._request_id(expected_request_id)
        if expected_request_id is not None and expected_id is None:
            return self.unknown(
                "",
                "Fabric ChatClef expected request identity is invalid.",
            )
        if mapped is None:
            return self.unknown(
                expected_id or "",
                "Fabric ChatClef submit result is not an object.",
            )

        status_payload = self._mapping_value(mapped.get("status"))
        observed_id = (
            None
            if status_payload is None
            else self._request_id(status_payload.get("request_id"))
        )
        unknown_id = expected_id or observed_id or ""
        if status_payload is None:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result status is missing or invalid.",
            )

        outer_ok = mapped.get("ok", _MISSING)
        nested_ok = status_payload.get("ok", _MISSING)
        if type(outer_ok) is not bool or type(nested_ok) is not bool:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result success flags are missing or invalid.",
            )
        if outer_ok is not nested_ok:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result success flags are inconsistent.",
            )

        if observed_id is None:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result request identity is missing or invalid.",
            )
        if expected_id is not None and observed_id != expected_id:
            return self.unknown(
                expected_id,
                "Fabric ChatClef submit result request identity is mismatched.",
            )
        if "request_id" in mapped:
            outer_request_id = self._request_id(mapped.get("request_id"))
            if outer_request_id is None or outer_request_id != observed_id:
                return self.unknown(
                    unknown_id,
                    "Fabric ChatClef submit result request identity mirrors disagree.",
                )

        status = self._status(status_payload.get("status", _MISSING))
        if status is None:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result status is unknown or invalid.",
            )
        if nested_ok is not status.ok:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result status and success flag are inconsistent.",
            )

        nested_error = self._error_code(
            status_payload.get("error_code", _MISSING)
        )
        outer_error = self._error_code(mapped.get("error", _MISSING))
        if nested_error is _MISSING or outer_error is _MISSING:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result error fields are missing or invalid.",
            )
        if nested_error != outer_error:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result error mirrors disagree.",
            )
        if status.ok and nested_error is not None:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef successful submit result includes an error code.",
            )

        nested_message = status_payload.get("message", _MISSING)
        outer_message = mapped.get("message", _MISSING)
        if type(nested_message) is not str or type(outer_message) is not str:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result messages are missing or invalid.",
            )
        if nested_message != outer_message:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result message mirrors disagree.",
            )

        nested_data = self._fresh_mapping(status_payload.get("data", _MISSING))
        outer_details = self._fresh_mapping(mapped.get("details", _MISSING))
        if nested_data is None or outer_details is None:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result details are missing or invalid.",
            )
        try:
            details_match = nested_data == outer_details
        except Exception:
            details_match = False
        if not details_match:
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result detail mirrors disagree.",
            )
        if not self._submission_outcome_is_consistent(status, nested_data):
            return self.unknown(
                unknown_id,
                "Fabric ChatClef submit result outcome metadata is inconsistent.",
            )

        return self._canonical_result(
            request_id=observed_id,
            ok=nested_ok,
            status=status,
            error_code=nested_error,
            message=nested_message,
            data=nested_data,
        )

    def unknown(
        self,
        expected_request_id: str | None,
        message: str,
        *,
        error: str = BridgeErrorCode.INTERNAL_ERROR.value,
    ) -> dict[str, Any]:
        request_id = self._request_id(expected_request_id) or ""
        error_code = (
            self._known_error_value(error)
            or BridgeErrorCode.INTERNAL_ERROR.value
        )
        data = {
            "submission_outcome": "submission_outcome_unknown",
            "reconciliation_required": True,
        }
        return self._canonical_result(
            request_id=request_id,
            ok=False,
            status=CommandResultStatus.UNKNOWN,
            error_code=error_code,
            message=str(message or "Fabric ChatClef submission outcome is unknown."),
            data=data,
        )

    def _canonical_result(
        self,
        *,
        request_id: str,
        ok: bool,
        status: CommandResultStatus,
        error_code: str | None,
        message: str,
        data: Mapping[str, Any],
    ) -> dict[str, Any]:
        status_data = copy.deepcopy(dict(data))
        outer_details = copy.deepcopy(dict(data))
        return {
            "ok": ok,
            "status": {
                "request_id": request_id,
                "ok": ok,
                "status": status.value,
                "error_code": error_code,
                "message": message,
                "data": status_data,
            },
            "error": error_code,
            "message": message,
            "details": outer_details,
        }

    def _submission_outcome_is_consistent(
        self,
        status: CommandResultStatus,
        data: Mapping[str, Any],
    ) -> bool:
        outcome = data.get("submission_outcome", _MISSING)
        reconciliation = data.get("reconciliation_required", _MISSING)
        if outcome is not _MISSING:
            if type(outcome) is not str or outcome not in self.SUBMISSION_OUTCOMES:
                return False
        if reconciliation is not _MISSING and type(reconciliation) is not bool:
            return False
        if status is CommandResultStatus.UNKNOWN:
            return (
                outcome == "submission_outcome_unknown"
                and reconciliation is True
            )
        if outcome == "submission_outcome_unknown" or reconciliation is True:
            return False
        if outcome == "accepted" and not status.ok:
            return False
        if outcome == "submit_response_not_accepted" and status.ok:
            return False
        return True

    def _mapping(self, payload: Any) -> dict[str, Any] | None:
        if isinstance(payload, Mapping):
            return self._mapping_value(payload)
        to_dict = getattr(payload, "to_dict", None)
        if not callable(to_dict):
            return None
        try:
            mapped = to_dict()
        except Exception:
            return None
        return self._mapping_value(mapped)

    def _mapping_value(self, value: Any) -> dict[str, Any] | None:
        if not isinstance(value, Mapping):
            return None
        try:
            return dict(value)
        except Exception:
            return None

    def _fresh_mapping(self, value: Any) -> dict[str, Any] | None:
        mapped = self._mapping_value(value)
        if mapped is None:
            return None
        try:
            return copy.deepcopy(mapped)
        except Exception:
            return None

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
        known = self._known_error_value(value)
        return _MISSING if known is None else known

    def _known_error_value(self, value: Any) -> str | None:
        if type(value) is not str:
            return None
        try:
            return BridgeErrorCode(value).value
        except ValueError:
            return None
