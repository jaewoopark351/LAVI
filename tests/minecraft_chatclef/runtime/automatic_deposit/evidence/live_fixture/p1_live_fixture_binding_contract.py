# 20260901_kpopmodder: Bind one sealed live fixture observation to exact P1 manifests.
from __future__ import annotations

from ..fixture_manifest import AutomaticDepositFixtureManifest
from ..run_manifest import AutomaticDepositRunManifest
from .p1_live_fixture_binding_verification import (
    P1LiveFixtureBindingVerification,
)
from .p1_live_fixture_observation import P1LiveFixtureObservation


_EXPECTED_SCHEMA = "p1-live-fixture-observation/v1"
_READY_VERDICT = "PASS"
_READY_REASON = "P1_LIVE_FIXTURE_READY"


def verify_p1_live_fixture_binding(
    observation: object,
    run_manifest: object,
    fixture_manifest: object,
) -> P1LiveFixtureBindingVerification:
    typed_observation = (
        observation if isinstance(observation, P1LiveFixtureObservation) else None
    )
    typed_run = (
        run_manifest if isinstance(run_manifest, AutomaticDepositRunManifest) else None
    )
    typed_fixture = (
        fixture_manifest
        if isinstance(fixture_manifest, AutomaticDepositFixtureManifest)
        else None
    )

    identity_errors: list[str] = []
    if typed_observation is None:
        identity_errors.append("P1_LIVE_FIXTURE_OBSERVATION_NOT_TYPED")
    if typed_run is None:
        identity_errors.append("P1_LIVE_FIXTURE_RUN_MANIFEST_NOT_TYPED")
    if typed_fixture is None:
        identity_errors.append("P1_LIVE_FIXTURE_MANIFEST_NOT_TYPED")

    if typed_observation is not None:
        if typed_observation.schema_version != _EXPECTED_SCHEMA:
            identity_errors.append("P1_LIVE_FIXTURE_SCHEMA_VERSION_INVALID")
    if typed_run is not None:
        if typed_run.row_id != "P1":
            identity_errors.append("P1_LIVE_FIXTURE_RUN_ROW_MISMATCH")
    if typed_fixture is not None:
        if typed_fixture.row_id != "P1":
            identity_errors.append("P1_LIVE_FIXTURE_MANIFEST_ROW_MISMATCH")

    if (
        typed_observation is not None
        and typed_run is not None
        and typed_fixture is not None
    ):
        identity_errors.extend(
            _identity_binding_errors(
                typed_observation,
                typed_run,
                typed_fixture,
            )
        )

    readiness_errors = (
        _readiness_errors(typed_observation) if typed_observation is not None else ()
    )
    identity_bound = not identity_errors
    ready = identity_bound and not readiness_errors
    reason = (
        "P1_LIVE_FIXTURE_BOUND_AND_READY"
        if ready
        else (
            "P1_LIVE_FIXTURE_NOT_READY"
            if identity_bound
            else "P1_LIVE_FIXTURE_BINDING_FAILED"
        )
    )
    limitations = (
        typed_observation.limitations
        if typed_observation is not None
        and isinstance(typed_observation.limitations, tuple)
        else ()
    )
    return P1LiveFixtureBindingVerification(
        ok=ready,
        identity_bound=identity_bound,
        ready=ready,
        reason=reason,
        observation=typed_observation,
        observation_fingerprint=(
            typed_observation.observation_fingerprint
            if typed_observation is not None
            else ""
        ),
        identity_errors=tuple(identity_errors),
        readiness_errors=readiness_errors,
        limitations=limitations,
    )


def _identity_binding_errors(
    observation: P1LiveFixtureObservation,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
) -> tuple[str, ...]:
    errors: list[str] = []
    if run_manifest.fixture_fingerprint != fixture_manifest.fixture_fingerprint:
        errors.append("P1_LIVE_FIXTURE_RUN_FIXTURE_FINGERPRINT_MISMATCH")
    if run_manifest.world != observation.world_key:
        errors.append("P1_LIVE_FIXTURE_RUN_WORLD_MISMATCH")
    if fixture_manifest.world_snapshot_id != observation.world_snapshot_fingerprint:
        errors.append("P1_LIVE_FIXTURE_WORLD_SNAPSHOT_MISMATCH")
    if fixture_manifest.inventory_snapshot_id != (
        observation.player_inventory_fingerprint
    ):
        errors.append("P1_LIVE_FIXTURE_INVENTORY_SNAPSHOT_MISMATCH")

    observed = observation.as_fixture_evidence_mapping()
    fixture = fixture_manifest.as_evidence_mapping()
    for key, reason in (
        (
            "trusted_destination_fingerprint",
            "P1_LIVE_FIXTURE_TRUSTED_DESTINATION_FINGERPRINT_MISMATCH",
        ),
        (
            "trusted_container_position",
            "P1_LIVE_FIXTURE_TRUSTED_POSITION_MISMATCH",
        ),
        ("container_type", "P1_LIVE_FIXTURE_CONTAINER_TYPE_MISMATCH"),
        (
            "trusted_binding_count",
            "P1_LIVE_FIXTURE_TRUSTED_BINDING_COUNT_MISMATCH",
        ),
    ):
        if fixture.get(key) != observed.get(key):
            errors.append(reason)
    return tuple(errors)


def _readiness_errors(
    observation: P1LiveFixtureObservation,
) -> tuple[str, ...]:
    errors: list[str] = []
    if observation.container_has_capacity is not True:
        errors.append("P1_LIVE_FIXTURE_CONTAINER_HAS_NO_CAPACITY")
    if observation.verdict == "INCONCLUSIVE":
        errors.append("P1_LIVE_FIXTURE_VERDICT_INCONCLUSIVE")
    elif observation.verdict != _READY_VERDICT:
        errors.append("P1_LIVE_FIXTURE_VERDICT_INVALID")
    elif observation.reason != _READY_REASON:
        errors.append("P1_LIVE_FIXTURE_READY_REASON_INVALID")
    if not _valid_limitations(observation.limitations):
        errors.append("P1_LIVE_FIXTURE_LIMITATIONS_INVALID")
    elif observation.limitations:
        errors.append("P1_LIVE_FIXTURE_LIMITATIONS_PRESENT")
    return tuple(errors)


def _valid_limitations(value: object) -> bool:
    return (
        isinstance(value, tuple)
        and len(value) <= 32
        and all(
            isinstance(item, str)
            and bool(item)
            and len(item) <= 160
            and not any(
                ord(character) < 0x20 or ord(character) == 0x7F for character in item
            )
            for item in value
        )
    )
