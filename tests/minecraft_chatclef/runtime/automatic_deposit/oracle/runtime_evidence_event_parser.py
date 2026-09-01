#20260831_kpopmodder: Require a complete versioned schema for runtime product evidence.
from __future__ import annotations

import re

from .diagnostic_event_parser import parse_bounded_diagnostic_event
from .runtime_evidence_event import AutomaticDepositRuntimeEvidenceEvent

SCHEMA_VERSION = "automatic-deposit-evidence/v1"
_REQUIRED_FIELDS = frozenset(
    {
        "schemaVersion",
        "event",
        "eventSequence",
        "runId",
        "rowId",
        "operationId",
        "artifactSha256",
        "fixtureFingerprint",
        "evidenceOwner",
        "evidenceKey",
        "evidenceValue",
        "diagnosticCaptureStatus",
    }
)
_OPTIONAL_FIELDS = frozenset({"gameTick"})


def is_automatic_deposit_evidence_candidate(line: str) -> bool:
    return (
        "schemaVersion=automatic-deposit-evidence" in line
        or "event=automaticDepositEvidence" in line
    )


def parse_automatic_deposit_runtime_evidence_event(
    line: object,
) -> tuple[AutomaticDepositRuntimeEvidenceEvent | None, str]:
    parsed = parse_bounded_diagnostic_event(line)
    if not parsed.ok:
        return None, parsed.reason
    fields = parsed.as_mapping()
    if parsed.marker != "[LAVI ChatClefBoundary]":
        return None, "RUNTIME_EVIDENCE_MARKER_INVALID"
    if set(fields) - (_REQUIRED_FIELDS | _OPTIONAL_FIELDS):
        return None, "RUNTIME_EVIDENCE_UNKNOWN_FIELD"
    missing = _REQUIRED_FIELDS - set(fields)
    if missing:
        return None, "RUNTIME_EVIDENCE_REQUIRED_FIELD_MISSING"
    if fields["schemaVersion"] != SCHEMA_VERSION:
        return None, "RUNTIME_EVIDENCE_SCHEMA_VERSION_INVALID"
    if fields["event"] != "automaticDepositEvidence":
        return None, "RUNTIME_EVIDENCE_EVENT_TYPE_INVALID"
    if fields["diagnosticCaptureStatus"] != "complete":
        return None, "RUNTIME_EVIDENCE_CAPTURE_NOT_COMPLETE"
    if not re.fullmatch(r"[1-9][0-9]{0,18}", fields["eventSequence"]):
        return None, "RUNTIME_EVIDENCE_SEQUENCE_INVALID"
    for key in ("runId", "rowId", "operationId"):
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._:-]{0,127}", fields[key]):
            return None, f"RUNTIME_EVIDENCE_{key.upper()}_INVALID"
    for key in ("artifactSha256", "fixtureFingerprint"):
        if not re.fullmatch(r"[0-9a-fA-F]{64}", fields[key]):
            return None, f"RUNTIME_EVIDENCE_{key.upper()}_INVALID"
    if fields["evidenceOwner"] != "RUNTIME_LOG":
        return None, "RUNTIME_EVIDENCE_OWNER_INVALID"
    if not re.fullmatch(
        r"[A-Za-z][A-Za-z0-9_.-]{0,95}", fields["evidenceKey"]
    ):
        return None, "RUNTIME_EVIDENCE_KEY_INVALID"
    if fields["evidenceValue"] not in ("true", "false") and not re.fullmatch(
        r"-?(0|[1-9][0-9]{0,18})|[A-Za-z][A-Za-z0-9_.:-]{0,127}",
        fields["evidenceValue"],
    ):
        return None, "RUNTIME_EVIDENCE_VALUE_INVALID"
    if "gameTick" in fields and not re.fullmatch(r"[0-9]{1,19}", fields["gameTick"]):
        return None, "RUNTIME_EVIDENCE_GAME_TICK_INVALID"
    return (
        AutomaticDepositRuntimeEvidenceEvent(
            sequence=int(fields["eventSequence"]),
            run_id=fields["runId"],
            row_id=fields["rowId"],
            operation_id=fields["operationId"],
            artifact_sha256=fields["artifactSha256"].lower(),
            fixture_fingerprint=fields["fixtureFingerprint"].lower(),
            owner=fields["evidenceOwner"],
            evidence_key=fields["evidenceKey"],
            evidence_value=fields["evidenceValue"],
        ),
        "RUNTIME_EVIDENCE_EVENT_PARSED",
    )
