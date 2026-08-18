#20260818_kpopmodder: Normalize one untrusted extension submit result fail-closed.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftChatClefSubmissionResultNormalizer:
    ACCEPTED_STATUSES = {"accepted", "running", "completed"}
    REJECTED_STATUSES = {
        "rejected",
        "failed",
        "cancelled",
        "deadline_exceeded",
    }

    def normalize(
        self,
        payload: Any,
        *,
        expected_request_id: str,
    ) -> dict[str, Any]:
        mapped = self._mapping(payload)
        if mapped is None:
            return self.unknown(
                expected_request_id,
                "Fabric ChatClef submit result is not an object.",
                error="malformed_submission_result",
            )
        ok = mapped.get("ok")
        status_payload = mapped.get("status")
        if type(ok) is not bool or not isinstance(status_payload, Mapping):
            return self.unknown(
                expected_request_id,
                "Fabric ChatClef submit result fields are missing or invalid.",
                error="malformed_submission_result",
            )
        status = str(status_payload.get("status") or "").strip().lower()
        request_id = str(status_payload.get("request_id") or "").strip()
        if request_id != expected_request_id or not status:
            return self.unknown(
                expected_request_id,
                "Fabric ChatClef submit result request identity is missing or mismatched.",
                error="malformed_submission_result",
            )
        if status == "unknown":
            return self._unknown_from_mapping(mapped, expected_request_id)
        if status in self.ACCEPTED_STATUSES and ok is True:
            return mapped
        if status in self.REJECTED_STATUSES and ok is False:
            return mapped
        return self.unknown(
            expected_request_id,
            "Fabric ChatClef submit result status and success flag are inconsistent.",
            error="malformed_submission_result",
        )

    def unknown(
        self,
        expected_request_id: str,
        message: str,
        *,
        error: str = "submission_outcome_unknown",
    ) -> dict[str, Any]:
        return {
            "ok": False,
            "status": {
                "request_id": expected_request_id,
                "ok": False,
                "status": "unknown",
                "error_code": "internal_error",
                "message": message,
                "data": {
                    "submission_outcome": "submission_outcome_unknown",
                    "reconciliation_required": True,
                },
            },
            "error": error,
            "message": message,
            "details": {
                "submission_outcome": "submission_outcome_unknown",
                "reconciliation_required": True,
            },
        }

    def _unknown_from_mapping(
        self,
        mapped: Mapping[str, Any],
        expected_request_id: str,
    ) -> dict[str, Any]:
        message = str(
            mapped.get("message")
            or "Fabric ChatClef submission outcome is unknown."
        ).strip()
        normalized = self.unknown(expected_request_id, message)
        details = mapped.get("details")
        if isinstance(details, Mapping):
            normalized["details"].update(dict(details))
            normalized["details"]["submission_outcome"] = (
                "submission_outcome_unknown"
            )
            normalized["details"]["reconciliation_required"] = True
        return normalized

    def _mapping(self, payload: Any) -> dict[str, Any] | None:
        if isinstance(payload, Mapping):
            return dict(payload)
        to_dict = getattr(payload, "to_dict", None)
        if not callable(to_dict):
            return None
        try:
            mapped = to_dict()
        except Exception:
            return None
        return dict(mapped) if isinstance(mapped, Mapping) else None
