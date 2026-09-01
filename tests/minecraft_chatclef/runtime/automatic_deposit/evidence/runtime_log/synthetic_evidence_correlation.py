#20260901_kpopmodder: Correlate the versioned synthetic evidence schema only for hermetic harness tests.
from __future__ import annotations

from ..fixture_manifest import AutomaticDepositFixtureManifest
from ..latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ..run_manifest import AutomaticDepositRunManifest
from .log_delta_contract import verify_automatic_deposit_runtime_log_delta
from ...oracle.runtime_evidence_event import AutomaticDepositRuntimeEvidenceEvent
from ...oracle.runtime_evidence_event_parser import (
    is_automatic_deposit_evidence_candidate,
    parse_automatic_deposit_runtime_evidence_event,
)
from ...scenario.matrix_row import AutomaticDepositMatrixRow


def verify_synthetic_runtime_log_delta(
    run_manifest: AutomaticDepositRunManifest,
    log_delta: AutomaticDepositLatestLogDeltaResult,
) -> tuple[str, ...]:
    return verify_automatic_deposit_runtime_log_delta(
        run_manifest.latest_log_cursor,
        log_delta,
    )


def parse_correlated_synthetic_runtime_events(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    log_delta: AutomaticDepositLatestLogDeltaResult,
) -> tuple[tuple[AutomaticDepositRuntimeEvidenceEvent, ...], tuple[str, ...]]:
    expected_requirements = {
        requirement.key: requirement
        for requirement in row.evidence_requirements
        if requirement.owner == "RUNTIME_LOG"
        and requirement.key != "runtime_log_complete"
    }
    events: list[AutomaticDepositRuntimeEvidenceEvent] = []
    errors: list[str] = []
    previous_sequence = 0
    for line in log_delta.text.splitlines():
        if not is_automatic_deposit_evidence_candidate(line):
            continue
        event, reason = parse_automatic_deposit_runtime_evidence_event(line)
        if event is None:
            errors.append(reason)
            continue
        if event.sequence <= previous_sequence:
            errors.append("RUNTIME_EVIDENCE_SEQUENCE_NOT_STRICTLY_INCREASING")
        previous_sequence = event.sequence
        if event.run_id != run_manifest.run_id:
            errors.append("RUNTIME_EVIDENCE_RUN_ID_MISMATCH")
        if event.row_id != row.row_id:
            errors.append("RUNTIME_EVIDENCE_ROW_ID_MISMATCH")
        if event.operation_id != run_manifest.operation_id:
            errors.append("RUNTIME_EVIDENCE_OPERATION_ID_MISMATCH")
        if (
            event.artifact_sha256
            != run_manifest.artifact_identity.deployed_jar_sha256.lower()
        ):
            errors.append("RUNTIME_EVIDENCE_ARTIFACT_SHA256_MISMATCH")
        if event.fixture_fingerprint != fixture_manifest.fixture_fingerprint.lower():
            errors.append("RUNTIME_EVIDENCE_FIXTURE_FINGERPRINT_MISMATCH")
        requirement = expected_requirements.get(event.evidence_key)
        if requirement is None:
            errors.append(f"RUNTIME_EVIDENCE_KEY_NOT_REQUIRED:{event.evidence_key}")
        elif event.owner != requirement.owner:
            errors.append(f"RUNTIME_EVIDENCE_OWNER_MISMATCH:{event.evidence_key}")
        if any(existing.evidence_key == event.evidence_key for existing in events):
            errors.append(f"RUNTIME_EVIDENCE_KEY_DUPLICATE:{event.evidence_key}")
        events.append(event)

    observed_keys = {event.evidence_key for event in events}
    errors.extend(
        f"RUNTIME_EVIDENCE_KEY_MISSING:{key}"
        for key in expected_requirements
        if key not in observed_keys
    )
    return tuple(events), tuple(errors)
