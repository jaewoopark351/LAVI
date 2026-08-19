#20260819_kpopmodder: Build fresh canonical and fail-closed UNKNOWN result mappings.
from __future__ import annotations

import copy
from typing import Any

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)

from .canonical_submission_result import CanonicalSubmissionResult


class SubmissionResultFactory:
    def canonical(self, result: CanonicalSubmissionResult) -> dict[str, Any]:
        status_data = copy.deepcopy(dict(result.data))
        outer_details = copy.deepcopy(dict(result.data))
        return {
            "request_id": result.request_id,
            "ok": result.ok,
            "status": {
                "request_id": result.request_id,
                "ok": result.ok,
                "status": result.status.value,
                "error_code": result.error_code,
                "message": result.message,
                "data": status_data,
            },
            "error": result.error_code,
            "message": result.message,
            "details": outer_details,
        }

    def unknown(
        self,
        expected_request_id: str | None,
        message: str,
        *,
        error: str = BridgeErrorCode.INTERNAL_ERROR.value,
    ) -> dict[str, Any]:
        request_id = self._request_id(expected_request_id) or ""
        error_code = self._known_error_value(error)
        if error_code is None:
            error_code = BridgeErrorCode.INTERNAL_ERROR.value
        return self.canonical(
            CanonicalSubmissionResult(
                request_id=request_id,
                ok=False,
                status=CommandResultStatus.UNKNOWN,
                error_code=error_code,
                message=str(
                    message
                    or "Fabric ChatClef submission outcome is unknown."
                ),
                data={
                    "submission_outcome": "submission_outcome_unknown",
                    "reconciliation_required": True,
                },
            )
        )

    def _request_id(self, value: Any) -> str | None:
        if type(value) is not str or not value or value != value.strip():
            return None
        return value

    def _known_error_value(self, value: Any) -> str | None:
        if type(value) is not str:
            return None
        try:
            return BridgeErrorCode(value).value
        except ValueError:
            return None
