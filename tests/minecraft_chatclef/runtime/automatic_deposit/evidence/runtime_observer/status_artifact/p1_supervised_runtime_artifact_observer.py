#20260901_kpopmodder: Parse only JVM-owned runtime artifact status bound to the active Fabric session.
from __future__ import annotations

import re
from collections.abc import Mapping
from pathlib import Path

from minecraft_chatclef.runtime.preflight.runtime_bridge_snapshot import (
    runtime_bridge_snapshot,
)

from ..production_status_observer import observe_production_fabric_status
from .p1_supervised_runtime_artifact_observation import (
    P1SupervisedRuntimeArtifactObservation,
    _create_p1_supervised_runtime_artifact_observation,
)


_SCHEMA = "p1-supervised-runtime-artifact/v1"
_BACKEND = "fabric_chatclef"
_LOADER = "fabric"
_MINECRAFT_VERSION = "1.20.1"
_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)
_SHA_SOURCES = frozenset(("JVM_COMPUTED", "JVM_VERIFIED"))
_ARTIFACT_KEYS = frozenset(
    (
        "schema_version",
        "backend_id",
        "loader_id",
        "minecraft_version",
        "chatclef_version",
        "loaded_jar_path",
        "loaded_jar_size",
        "loaded_jar_sha256",
        "sha256_evidence_source",
        "minecraft_process_id",
        "session_id",
        "connection_generation",
    )
)


def observe_p1_supervised_runtime_artifact(
    payload: object,
) -> tuple[P1SupervisedRuntimeArtifactObservation | None, str]:
    status, status_reason = observe_production_fabric_status(payload)
    if status is None:
        return None, f"P1_SUPERVISED_RUNTIME_STATUS_NOT_VERIFIED:{status_reason}"
    bridge, bridge_error = runtime_bridge_snapshot(payload)
    if bridge_error:
        return None, f"P1_SUPERVISED_RUNTIME_STATUS_NOT_VERIFIED:{bridge_error}"
    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_DETAILS_MISSING"
    raw = details.get("runtime_artifact")
    if not isinstance(raw, Mapping):
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_STATUS_MISSING"
    artifact = dict(raw)
    if frozenset(artifact) != _ARTIFACT_KEYS:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_SCHEMA_INVALID"
    if artifact.get("schema_version") != _SCHEMA:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_VERSION_INVALID"
    if _text(artifact.get("backend_id")) != _BACKEND:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_BACKEND_INVALID"
    if _text(artifact.get("loader_id")) != _LOADER:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_LOADER_INVALID"
    if _text(artifact.get("minecraft_version")) != _MINECRAFT_VERSION:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_MINECRAFT_VERSION_INVALID"
    chatclef_version = _bounded_text(artifact.get("chatclef_version"), 128)
    if not chatclef_version:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_CHATCLEF_VERSION_INVALID"
    loaded_jar_path = _absolute_path(artifact.get("loaded_jar_path"))
    if not loaded_jar_path:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_JAR_PATH_INVALID"
    loaded_jar_size = artifact.get("loaded_jar_size")
    if type(loaded_jar_size) is not int or loaded_jar_size <= 0:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_JAR_SIZE_INVALID"
    loaded_jar_sha256 = _text(artifact.get("loaded_jar_sha256"))
    if _SHA256.fullmatch(loaded_jar_sha256) is None:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_SHA256_INVALID"
    sha_source = _text(artifact.get("sha256_evidence_source"))
    if sha_source not in _SHA_SOURCES:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_SHA256_SOURCE_INVALID"
    minecraft_process_id = artifact.get("minecraft_process_id")
    if type(minecraft_process_id) is not int or minecraft_process_id <= 0:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_PROCESS_ID_INVALID"
    session_id = _bounded_text(artifact.get("session_id"), 256)
    if session_id != status.session_id:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_SESSION_MISMATCH"
    generation = artifact.get("connection_generation")
    if type(generation) is not int or generation != status.connection_generation:
        return None, "P1_SUPERVISED_RUNTIME_ARTIFACT_GENERATION_MISMATCH"
    return (
        _create_p1_supervised_runtime_artifact_observation(
            backend_id=_BACKEND,
            loader_id=_LOADER,
            minecraft_version=_MINECRAFT_VERSION,
            chatclef_version=chatclef_version,
            loaded_jar_path=loaded_jar_path,
            loaded_jar_size=loaded_jar_size,
            loaded_jar_sha256=loaded_jar_sha256.casefold(),
            sha256_evidence_source=sha_source,
            minecraft_process_id=minecraft_process_id,
            session_id=session_id,
            connection_generation=generation,
        ),
        "P1_SUPERVISED_RUNTIME_ARTIFACT_OBSERVED",
    )


def _absolute_path(value: object) -> str:
    text = _text(value)
    if not text:
        return ""
    try:
        path = Path(text)
        if not path.is_absolute():
            return ""
        return str(path.resolve(strict=False))
    except (OSError, ValueError):
        return ""


def _bounded_text(value: object, maximum: int) -> str:
    text = _text(value)
    if not text or len(text) > maximum:
        return ""
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in text):
        return ""
    return text


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""
