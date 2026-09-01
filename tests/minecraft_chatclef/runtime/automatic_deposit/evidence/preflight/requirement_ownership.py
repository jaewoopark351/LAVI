#20260901_kpopmodder: Verify evidence requirement keys and their reserved owners.
from __future__ import annotations

from ...scenario.matrix_row import AutomaticDepositMatrixRow


def verify_requirement_ownership(row: AutomaticDepositMatrixRow) -> tuple[str, ...]:
    errors: list[str] = []
    keys = tuple(requirement.key for requirement in row.evidence_requirements)
    if len(keys) != len(set(keys)):
        errors.append("EVIDENCE_REQUIREMENT_KEY_DUPLICATE")
    reserved_owners = {
        "artifact_identity_verified": "ARTIFACT_PREFLIGHT",
        "fixture_identity_verified": "OPERATOR_FIXTURE",
        "runtime_log_complete": "RUNTIME_LOG",
        "runtime_reported_completion": "RUNTIME_LOG",
        "helper_submit_call_count": "HARNESS_CONTROL",
        "gameplay_observation_complete": "GAMEPLAY_OBSERVATION",
        "expected_gameplay_effect_verified": "GAMEPLAY_OBSERVATION",
        "partial_gameplay_effect_observed": "GAMEPLAY_OBSERVATION",
        "unexpected_effect_observed": "GAMEPLAY_OBSERVATION",
        "prohibited_effect_absence_verified": "GAMEPLAY_OBSERVATION",
    }
    for requirement in row.evidence_requirements:
        expected_owner = reserved_owners.get(requirement.key)
        if expected_owner is not None and requirement.owner != expected_owner:
            errors.append(f"EVIDENCE_REQUIREMENT_OWNER_INVALID:{requirement.key}")
    return tuple(errors)
