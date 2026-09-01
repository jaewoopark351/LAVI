#20260901_kpopmodder: Require typed JVM artifact and one exact trusted destination before gateway creation.
from __future__ import annotations

from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observation import (
    P1SupervisedRuntimeArtifactObservation,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)

from ..request.p1_supervised_execution_request import (
    P1SupervisedExecutionRequest,
)
def verify_p1_supervised_pre_submit_evidence(
    request: P1SupervisedExecutionRequest,
    runtime_artifact: object,
    runtime_artifact_reason: object,
    trusted_destination: object,
    trusted_destination_reason: object,
) -> str:
    if not isinstance(runtime_artifact, P1SupervisedRuntimeArtifactObservation):
        return "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_VERIFIED"
    if runtime_artifact_reason != "P1_SUPERVISED_RUNTIME_ARTIFACT_OBSERVED":
        return "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_VERIFIED"
    if runtime_artifact.backend_id != request.expected_backend:
        return "P1_SUPERVISED_RUNTIME_ARTIFACT_BACKEND_MISMATCH"
    if not isinstance(trusted_destination, P1LiveFixtureObservation):
        if _reason(trusted_destination_reason):
            return trusted_destination_reason
        return "P1_SUPERVISED_LIVE_FIXTURE_NOT_VERIFIED"
    if (
        trusted_destination_reason != "P1_LIVE_FIXTURE_READY"
        or trusted_destination.schema_version
        != "p1-live-fixture-observation/v1"
        or trusted_destination.verdict != "PASS"
        or trusted_destination.reason != "P1_LIVE_FIXTURE_READY"
        or trusted_destination.limitations
    ):
        return "P1_SUPERVISED_FIXTURE_NOT_READY"
    if trusted_destination.world_key != request.expected_world:
        return "P1_SUPERVISED_TRUSTED_DESTINATION_WORLD_MISMATCH"
    if trusted_destination.dimension != "OVERWORLD":
        return "P1_SUPERVISED_TRUSTED_DESTINATION_DIMENSION_MISMATCH"
    candidate_position = ", ".join(
        str(value) for value in trusted_destination.trusted_position
    )
    if candidate_position != request.expected_candidate_position:
        return "P1_SUPERVISED_TRUSTED_DESTINATION_POSITION_MISMATCH"
    if not _identity(trusted_destination.trusted_destination_id):
        return "P1_SUPERVISED_TRUSTED_DESTINATION_ID_INVALID"
    if trusted_destination.container_has_capacity is not True:
        return "P1_SUPERVISED_TRUSTED_CONTAINER_HAS_NO_CAPACITY"
    if not trusted_destination.player_inventory_counts or any(
        type(count) is not int or count <= 0
        for _item_id, count in trusted_destination.player_inventory_counts
    ):
        return "P1_SUPERVISED_PLAYER_INVENTORY_NOT_VERIFIED"
    return ""


def _reason(value: object) -> bool:
    return (
        type(value) is str
        and value == value.strip()
        and 0 < len(value) <= 512
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
    )


def _identity(value: object) -> bool:
    return _reason(value) and value.casefold() not in {
        "none",
        "null",
        "unavailable",
        "unverified",
    }
