# 20260901_kpopmodder: Classify static JVM evidence without promoting it to P1 operation evidence.
from __future__ import annotations

from ....runtime_observer.jvm_runtime_artifact_observation import (
    JvmRuntimeArtifactObservation,
)
from ....runtime_observer.production_status_observation import (
    ProductionFabricStatusObservation,
)
from ....runtime_observer.runtime_observation_binder import (
    bind_runtime_observations,
)
from .static_manifest_binding import (
    COMMAND_CONTEXT_UNAVAILABLE,
    DIRECT,
    HISTORICAL_GENERATION,
    HISTORICAL_SESSION,
    P1StaticManifestBinding,
    _create_p1_static_manifest_binding,
)


def classify_p1_static_manifest_binding(
    status: object,
    raw_artifact: object,
) -> tuple[P1StaticManifestBinding | None, str]:
    binding, binding_reason = bind_runtime_observations(status, raw_artifact)
    if not isinstance(status, ProductionFabricStatusObservation):
        return None, binding_reason
    if not isinstance(raw_artifact, JvmRuntimeArtifactObservation):
        return None, binding_reason

    source_binding_fingerprint = ""
    if binding_reason == "RUNTIME_OBSERVATIONS_DIRECTLY_BOUND":
        if binding is None or not binding.directly_bound:
            return None, "P1_STATIC_MANIFEST_DIRECT_BINDING_INVALID"
        classification = DIRECT
        reason = binding_reason
        source_binding_fingerprint = binding.binding_fingerprint
    elif binding_reason == "RUNTIME_OBSERVATIONS_NOT_DIRECTLY_BOUND":
        if (
            binding is None
            or binding.directly_bound
            or binding.limitation_reason != "MANIFEST_COMMAND_CONTEXT_UNAVAILABLE"
        ):
            return None, "P1_STATIC_MANIFEST_LIMITATION_BINDING_INVALID"
        classification = COMMAND_CONTEXT_UNAVAILABLE
        reason = binding.limitation_reason
        source_binding_fingerprint = binding.binding_fingerprint
    elif binding_reason == "RUNTIME_OBSERVATION_SESSION_BINDING_MISMATCH":
        if binding is not None:
            return None, "P1_STATIC_MANIFEST_HISTORICAL_SESSION_INVALID"
        classification = HISTORICAL_SESSION
        reason = binding_reason
    elif binding_reason == "RUNTIME_OBSERVATION_GENERATION_BINDING_MISMATCH":
        if binding is not None:
            return None, "P1_STATIC_MANIFEST_HISTORICAL_GENERATION_INVALID"
        classification = HISTORICAL_GENERATION
        reason = binding_reason
    else:
        return None, f"P1_STATIC_MANIFEST_BINDING_INVALID:{binding_reason}"

    return (
        _create_p1_static_manifest_binding(
            classification=classification,
            reason=reason,
            status_fingerprint=status.observation_fingerprint,
            raw_artifact_fingerprint=raw_artifact.observation_fingerprint,
            source_runtime_binding_fingerprint=source_binding_fingerprint,
            status_session_id=status.session_id,
            status_connection_generation=status.connection_generation,
            artifact_session_id=raw_artifact.command_session_id,
            artifact_connection_generation=(
                raw_artifact.command_connection_generation
            ),
        ),
        "P1_STATIC_MANIFEST_BINDING_CLASSIFIED",
    )
