#20260831_kpopmodder: Decide rows only from builder-sealed evidence and a verified fixture.
from __future__ import annotations

from ...observation.gameplay_outcome import evaluate_end_to_end_success
from ..evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ..evidence.fixture_manifest_contract import (
    verify_automatic_deposit_fixture_manifest,
)
from ..evidence.verified_evidence import AutomaticDepositVerifiedEvidence
from ..scenario.matrix_row import AutomaticDepositMatrixRow
from .matrix_verdict import AutomaticDepositVerdict
from .verdict_result import AutomaticDepositVerdictResult

_HANDOFF_KEYS = frozenset(
    {
        "placement_observed",
        "post_place_world_reread_observed",
        "container_open_observed",
        "transfer_observed",
        "typed_terminal_observed",
        "generation_boundary_verified",
        "one_tick_barrier_verified",
    }
)


def evaluate_automatic_deposit_row(
    row: AutomaticDepositMatrixRow,
    fixture_manifest: AutomaticDepositFixtureManifest,
    evidence: AutomaticDepositVerifiedEvidence,
    *,
    helper_submit_call_count: int,
) -> AutomaticDepositVerdictResult:
    if not isinstance(fixture_manifest, AutomaticDepositFixtureManifest):
        return _inconclusive("FIXTURE_MANIFEST_NOT_TYPED")
    if not isinstance(evidence, AutomaticDepositVerifiedEvidence):
        return _inconclusive("VERIFIED_EVIDENCE_NOT_TYPED")
    fixture_verification = verify_automatic_deposit_fixture_manifest(
        fixture_manifest,
        row,
    )
    if not fixture_verification.ok:
        return AutomaticDepositVerdictResult(
            AutomaticDepositVerdict.INCONCLUSIVE,
            "FIXTURE_MANIFEST_NOT_VERIFIED",
            violated_contracts=fixture_verification.errors,
        )
    if evidence.row_id != row.row_id:
        return _inconclusive("VERIFIED_EVIDENCE_ROW_ID_MISMATCH")
    if evidence.fixture_fingerprint != fixture_manifest.fixture_fingerprint.lower():
        return _inconclusive("VERIFIED_EVIDENCE_FIXTURE_MISMATCH")
    if helper_submit_call_count not in (0, 1):
        return _inconclusive("HELPER_SUBMIT_CALL_COUNT_INVALID")

    values = evidence.as_mapping()
    if "helper_submit_call_count" in values:
        return _inconclusive("VERIFIED_EVIDENCE_SHADOWS_SUBMIT_COUNT")
    values["helper_submit_call_count"] = helper_submit_call_count

    missing = tuple(
        requirement.key
        for requirement in row.evidence_requirements
        if requirement.key not in values
    )
    if missing:
        return AutomaticDepositVerdictResult(
            AutomaticDepositVerdict.INCONCLUSIVE,
            "REQUIRED_VERIFIED_EVIDENCE_MISSING",
            missing_evidence=missing,
        )
    gameplay_result = evaluate_end_to_end_success(values)
    if gameplay_result is None:
        return _inconclusive("GAMEPLAY_EVIDENCE_INCOMPLETE")
    if gameplay_result is False:
        return AutomaticDepositVerdictResult(
            AutomaticDepositVerdict.FAIL,
            "GAMEPLAY_CONTRACT_VIOLATED",
        )
    mismatched = tuple(
        requirement.key
        for requirement in row.evidence_requirements
        if not _matches_expected(
            values.get(requirement.key),
            requirement.expected_value,
        )
    )
    if mismatched:
        handoff_mismatches = tuple(key for key in mismatched if key in _HANDOFF_KEYS)
        if row.exercises_frozen_handoff and handoff_mismatches:
            return AutomaticDepositVerdictResult(
                AutomaticDepositVerdict.HANDOFF_DEFECT_FOUND,
                "FROZEN_HANDOFF_CONTRACT_VIOLATED",
                violated_contracts=handoff_mismatches,
            )
        return AutomaticDepositVerdictResult(
            AutomaticDepositVerdict.FAIL,
            "EXPECTED_RUNTIME_EVIDENCE_NOT_OBSERVED",
            violated_contracts=mismatched,
        )

    return AutomaticDepositVerdictResult(
        AutomaticDepositVerdict.PASS,
        "ALL_REQUIRED_EVIDENCE_VERIFIED",
    )


def _matches_expected(value: object, expected: str) -> bool:
    if expected == "present":
        return _required_value_present(value)
    if expected == "true":
        return value is True
    if expected == "false":
        return value is False
    return str(value).strip() == expected


def _required_value_present(value: object) -> bool:
    if value is None or value is False:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    return True


def _inconclusive(reason: str) -> AutomaticDepositVerdictResult:
    return AutomaticDepositVerdictResult(
        AutomaticDepositVerdict.INCONCLUSIVE,
        reason,
    )
