#20260901_kpopmodder: Bind only a real manifest command session/generation to status.
from __future__ import annotations

from .jvm_runtime_artifact_observation import JvmRuntimeArtifactObservation
from .production_status_observation import ProductionFabricStatusObservation
from .runtime_observation_binding import (
    RuntimeObservationBinding,
    _create_runtime_observation_binding,
)


def bind_runtime_observations(
    status: object,
    artifact: object,
) -> tuple[RuntimeObservationBinding | None, str]:
    if not isinstance(status, ProductionFabricStatusObservation):
        return None, "RUNTIME_STATUS_OBSERVATION_NOT_TYPED"
    if not isinstance(artifact, JvmRuntimeArtifactObservation):
        return None, "RUNTIME_ARTIFACT_OBSERVATION_NOT_TYPED"
    if not artifact.command_context_available:
        return (
            _create_runtime_observation_binding(
                status_observation_fingerprint=status.observation_fingerprint,
                artifact_observation_fingerprint=artifact.observation_fingerprint,
                directly_bound=False,
                limitation_reason="MANIFEST_COMMAND_CONTEXT_UNAVAILABLE",
                session_id="",
                connection_generation=None,
            ),
            "RUNTIME_OBSERVATIONS_NOT_DIRECTLY_BOUND",
        )
    if artifact.command_session_id != status.session_id:
        return None, "RUNTIME_OBSERVATION_SESSION_BINDING_MISMATCH"
    if artifact.command_connection_generation != status.connection_generation:
        return None, "RUNTIME_OBSERVATION_GENERATION_BINDING_MISMATCH"
    return (
        _create_runtime_observation_binding(
            status_observation_fingerprint=status.observation_fingerprint,
            artifact_observation_fingerprint=artifact.observation_fingerprint,
            directly_bound=True,
            limitation_reason="",
            session_id=status.session_id,
            connection_generation=status.connection_generation,
        ),
        "RUNTIME_OBSERVATIONS_DIRECTLY_BOUND",
    )
