# 20260901_kpopmodder: Reject contradictory static-manifest command context before classification.
from __future__ import annotations

from ....runtime_observer.jvm_runtime_artifact_observation import (
    JvmRuntimeArtifactObservation,
)


_RUN_MANIFEST_ID_SOURCES = frozenset(
    {"RUNTIME_GENERATED", "EXTERNAL_BUILD_DEPLOY_MANIFEST"}
)


def verify_p1_static_manifest_context(
    raw_artifact: object,
) -> tuple[str, ...]:
    if not isinstance(raw_artifact, JvmRuntimeArtifactObservation):
        return ("P1_STATIC_MANIFEST_ARTIFACT_NOT_TYPED",)
    if not _bounded_identity(raw_artifact.run_manifest_id, 256):
        return ("P1_STATIC_MANIFEST_RUN_MANIFEST_ID_INVALID",)
    if raw_artifact.run_manifest_id_source not in _RUN_MANIFEST_ID_SOURCES:
        return ("P1_STATIC_MANIFEST_RUN_MANIFEST_ID_SOURCE_INVALID",)
    if (
        type(raw_artifact.manifest_event_sequence) is not int
        or raw_artifact.manifest_event_sequence < 0
    ):
        return ("P1_STATIC_MANIFEST_EVENT_SEQUENCE_INVALID",)
    if type(raw_artifact.command_context_available) is not bool:
        return ("P1_STATIC_MANIFEST_COMMAND_CONTEXT_FLAG_INVALID",)
    if not raw_artifact.command_context_available:
        if (
            raw_artifact.command_session_id != ""
            or raw_artifact.command_connection_generation is not None
        ):
            return ("P1_STATIC_MANIFEST_COMMAND_CONTEXT_CONTRADICTORY",)
        return ()
    if not _bounded_identity(raw_artifact.command_session_id, 256):
        return ("P1_STATIC_MANIFEST_COMMAND_SESSION_ID_INVALID",)
    generation = raw_artifact.command_connection_generation
    if type(generation) is not int or not 1 <= generation <= 9_223_372_036_854_775_807:
        return ("P1_STATIC_MANIFEST_COMMAND_GENERATION_INVALID",)
    return ()


def _bounded_identity(value: object, maximum: int) -> bool:
    return (
        isinstance(value, str)
        and bool(value)
        and len(value) <= maximum
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
        and value.casefold() not in {"unavailable", "unverified"}
    )
