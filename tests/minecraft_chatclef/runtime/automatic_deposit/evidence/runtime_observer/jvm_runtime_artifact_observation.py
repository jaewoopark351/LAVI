#20260901_kpopmodder: Seal one independently hashed JVM CodeSource observation.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_JVM_RUNTIME_ARTIFACT_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class JvmRuntimeArtifactObservation:
    run_manifest_id: str
    run_manifest_id_source: str
    manifest_event_sequence: int
    runtime_code_source_uri: str
    runtime_code_source_path: str
    runtime_code_source_sha256: str
    runtime_code_source_size: int
    runtime_code_source_mtime_ns: int
    sha256_evidence_source: str
    reported_runtime_jar_sha256: str
    minecraft_version: str
    fabric_loader_version: str
    chatclef_version: str
    command_context_available: bool
    command_session_id: str
    command_connection_generation: int | None
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(self)
        if self._seal is not _JVM_RUNTIME_ARTIFACT_OBSERVATION_SEAL:
            raise ValueError("JVM runtime artifact observation must be created by its observer")
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("JVM runtime artifact observation integrity mismatch")


def _create_jvm_runtime_artifact_observation(
    *,
    run_manifest_id: str,
    run_manifest_id_source: str,
    manifest_event_sequence: int,
    runtime_code_source_uri: str,
    runtime_code_source_path: str,
    runtime_code_source_sha256: str,
    runtime_code_source_size: int,
    runtime_code_source_mtime_ns: int,
    reported_runtime_jar_sha256: str,
    minecraft_version: str,
    fabric_loader_version: str,
    chatclef_version: str,
    command_context_available: bool,
    command_session_id: str,
    command_connection_generation: int | None,
) -> JvmRuntimeArtifactObservation:
    provisional = JvmRuntimeArtifactObservation.__new__(JvmRuntimeArtifactObservation)
    values = {
        "run_manifest_id": run_manifest_id,
        "run_manifest_id_source": run_manifest_id_source,
        "manifest_event_sequence": manifest_event_sequence,
        "runtime_code_source_uri": runtime_code_source_uri,
        "runtime_code_source_path": runtime_code_source_path,
        "runtime_code_source_sha256": runtime_code_source_sha256,
        "runtime_code_source_size": runtime_code_source_size,
        "runtime_code_source_mtime_ns": runtime_code_source_mtime_ns,
        "sha256_evidence_source": "INDEPENDENT_STABLE_CODESOURCE_FILE_READ",
        "reported_runtime_jar_sha256": reported_runtime_jar_sha256,
        "minecraft_version": minecraft_version,
        "fabric_loader_version": fabric_loader_version,
        "chatclef_version": chatclef_version,
        "command_context_available": command_context_available,
        "command_session_id": command_session_id,
        "command_connection_generation": command_connection_generation,
    }
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return JvmRuntimeArtifactObservation(
        **values,
        observation_fingerprint=fingerprint,
        _seal=_JVM_RUNTIME_ARTIFACT_OBSERVATION_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(observation: JvmRuntimeArtifactObservation) -> str:
    payload = json.dumps(
        {
            "chatclef_version": observation.chatclef_version,
            "command_connection_generation": (
                observation.command_connection_generation
            ),
            "command_context_available": observation.command_context_available,
            "command_session_id": observation.command_session_id,
            "fabric_loader_version": observation.fabric_loader_version,
            "manifest_event_sequence": observation.manifest_event_sequence,
            "minecraft_version": observation.minecraft_version,
            "reported_runtime_jar_sha256": (
                observation.reported_runtime_jar_sha256
            ),
            "run_manifest_id": observation.run_manifest_id,
            "run_manifest_id_source": observation.run_manifest_id_source,
            "runtime_code_source_mtime_ns": (
                observation.runtime_code_source_mtime_ns
            ),
            "runtime_code_source_path": observation.runtime_code_source_path,
            "runtime_code_source_sha256": observation.runtime_code_source_sha256,
            "runtime_code_source_size": observation.runtime_code_source_size,
            "runtime_code_source_uri": observation.runtime_code_source_uri,
            "sha256_evidence_source": observation.sha256_evidence_source,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
