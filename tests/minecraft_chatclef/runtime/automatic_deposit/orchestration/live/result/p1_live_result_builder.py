#20260901_kpopmodder: Project only bounded path-free P1 live result details.
from __future__ import annotations

from collections.abc import Iterable

from ....evidence.live_fixture.p1_live_fixture_binding_verification import (
    P1LiveFixtureBindingVerification,
)
from ..p1_guard_gate_result import P1GuardGateResult
from ..p1_read_only_observer_call_result import P1ReadOnlyObserverCallResult
from .p1_live_detail_sanitizer import (
    bounded_p1_details,
    is_safe_p1_detail,
    is_safe_p1_error_type,
)


def p1_guard_failure_result(
    base: dict[str, object],
    gate: P1GuardGateResult,
) -> dict[str, object]:
    result = _base_failure(base, gate.reason)
    if is_safe_p1_detail(gate.guard_state):
        result["guard_state"] = gate.guard_state
    if is_safe_p1_detail(gate.guard_reason):
        result["guard_reason"] = gate.guard_reason
    if is_safe_p1_error_type(gate.error_type):
        result["guard_error"] = gate.error_type
    return result


def p1_observer_failure_result(
    base: dict[str, object],
    observation: P1ReadOnlyObserverCallResult,
) -> dict[str, object]:
    result = _base_failure(base, observation.reason)
    result["observer_name"] = observation.observer_name
    if is_safe_p1_detail(observation.source_reason):
        result["observer_source_reason"] = observation.source_reason
    if is_safe_p1_error_type(observation.error_type):
        result["observer_error_type"] = observation.error_type
    return result


def p1_contract_failure_result(
    base: dict[str, object],
    reason: str,
    errors: Iterable[object],
) -> dict[str, object]:
    result = _base_failure(base, reason)
    result["violated_contracts"] = bounded_p1_details(errors)
    return result


def p1_fixture_failure_result(
    base: dict[str, object],
    verification: P1LiveFixtureBindingVerification,
) -> dict[str, object]:
    result = p1_contract_failure_result(
        base,
        verification.reason,
        verification.errors,
    )
    result.update(
        {
            "fixture_identity_bound": verification.identity_bound,
            "fixture_ready": verification.ready,
            "fixture_observation_fingerprint": (
                verification.observation_fingerprint
            ),
            "fixture_limitations": bounded_p1_details(verification.limitations),
        }
    )
    return result


def p1_simple_failure_result(
    base: dict[str, object],
    reason: str,
    **details: object,
) -> dict[str, object]:
    return {**_base_failure(base, reason), **details}


def _base_failure(
    base: dict[str, object],
    reason: str,
) -> dict[str, object]:
    return {**base, "reason": reason, "submit_call_count": 0}
