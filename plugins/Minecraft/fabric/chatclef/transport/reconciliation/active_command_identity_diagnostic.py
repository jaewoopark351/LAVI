#20260820_kpopmodder: Evaluate exact active-command identity without mutating ownership.
from __future__ import annotations

from typing import Any, Mapping

from .reconciliation_mapping_utils import first_text, first_value, mapping, text


class ActiveCommandIdentityDiagnosticBuilder:
    def build(
        self,
        commands: Mapping[str, Any],
        last_result: Mapping[str, Any],
        last_data: Mapping[str, Any],
    ) -> dict[str, Any]:
        ownership = mapping(last_data.get("ownership"))
        observed_session_id = first_text(
            last_data,
            ("session_id", "active_session_id"),
        ) or first_text(ownership, ("session_id", "active_session_id"))
        observed_generation = (
            first_value(last_data, ("connection_generation", "active_generation"))
            if first_value(
                last_data,
                ("connection_generation", "active_generation"),
            )
            is not None
            else first_value(
                ownership,
                ("connection_generation", "active_generation"),
            )
        )
        observed_correlation = first_text(
            last_data,
            ("command_message_id", "correlation_id"),
        ) or first_text(
            ownership,
            ("command_message_id", "correlation_id"),
        )
        fields = {
            "request_id": (
                commands.get("active_request_id"),
                last_result.get("request_id") or ownership.get("request_id"),
            ),
            "session_id": (
                commands.get("active_session_id"),
                observed_session_id,
            ),
            "connection_generation": (
                commands.get("active_generation"),
                observed_generation,
            ),
            "command_message_id": (
                commands.get("active_command_message_id"),
                observed_correlation,
            ),
        }
        checks: dict[str, dict[str, Any]] = {}
        missing_fields: list[str] = []
        mismatch_fields: list[str] = []
        for field_name, values in fields.items():
            expected, observed = values
            check = _identity_check(expected, observed)
            checks[field_name] = check
            if check["result"] == "MISSING":
                missing_fields.append(field_name)
            elif check["result"] == "MISMATCH":
                mismatch_fields.append(field_name)

        if mismatch_fields:
            quality = "MISMATCH"
        elif missing_fields:
            quality = "PARTIAL"
        elif checks:
            quality = "EXACT"
        else:
            quality = "UNKNOWN"
        return {
            "quality": quality,
            "checks": checks,
            "missing_fields": missing_fields,
            "mismatch_fields": mismatch_fields,
            "accepted_evidence_provenance": {
                "accepted": bool(last_result),
                "accepted_request_id": last_result.get("request_id")
                or ownership.get("request_id"),
                "accepted_session_id": observed_session_id,
                "accepted_connection_generation": observed_generation,
                "accepted_correlation_id": observed_correlation,
                "accepted_at_ms": ownership.get("accepted_at_ms"),
            },
        }


def _identity_check(expected: Any, observed: Any) -> dict[str, Any]:
    expected_text = text(expected)
    observed_text = text(observed)
    if expected_text is None or observed_text is None:
        result = "MISSING"
    elif expected_text == observed_text:
        result = "MATCH"
    else:
        result = "MISMATCH"
    return {
        "expected": expected,
        "observed": observed,
        "result": result,
    }
