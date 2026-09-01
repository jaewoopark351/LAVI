# 20260901_kpopmodder: Seal raw JVM artifact evidence with its exact pre-action log-prefix provenance.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from ..jvm_runtime_artifact_observation import JvmRuntimeArtifactObservation


_JVM_LOG_PREFIX_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class JvmRuntimeArtifactLogPrefixObservation:
    runtime_artifact_observation: JvmRuntimeArtifactObservation
    source_log_path: str
    source_cursor_fingerprint: str
    source_prefix_sha256: str
    source_prefix_size: int
    source_device: int
    source_inode: int
    manifest_record_sequence: int
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if not isinstance(
            self.runtime_artifact_observation,
            JvmRuntimeArtifactObservation,
        ):
            raise ValueError("raw JVM runtime artifact observation must be sealed")
        if (
            self.manifest_record_sequence
            != self.runtime_artifact_observation.manifest_event_sequence
        ):
            raise ValueError("log-prefix manifest sequence mismatch")
        expected = _fingerprint(self)
        if self._seal is not _JVM_LOG_PREFIX_OBSERVATION_SEAL:
            raise ValueError("JVM log-prefix observation must be created by its observer")
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("JVM log-prefix observation integrity mismatch")


def _create_jvm_runtime_artifact_log_prefix_observation(
    *,
    runtime_artifact_observation: JvmRuntimeArtifactObservation,
    source_log_path: str,
    source_cursor_fingerprint: str,
    source_prefix_sha256: str,
    source_prefix_size: int,
    source_device: int,
    source_inode: int,
) -> JvmRuntimeArtifactLogPrefixObservation:
    sequence = runtime_artifact_observation.manifest_event_sequence
    provisional = JvmRuntimeArtifactLogPrefixObservation.__new__(
        JvmRuntimeArtifactLogPrefixObservation
    )
    values = {
        "runtime_artifact_observation": runtime_artifact_observation,
        "source_log_path": source_log_path,
        "source_cursor_fingerprint": source_cursor_fingerprint,
        "source_prefix_sha256": source_prefix_sha256,
        "source_prefix_size": source_prefix_size,
        "source_device": source_device,
        "source_inode": source_inode,
        "manifest_record_sequence": sequence,
    }
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return JvmRuntimeArtifactLogPrefixObservation(
        **values,
        observation_fingerprint=fingerprint,
        _seal=_JVM_LOG_PREFIX_OBSERVATION_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(observation: JvmRuntimeArtifactLogPrefixObservation) -> str:
    payload = json.dumps(
        {
            "manifest_record_sequence": observation.manifest_record_sequence,
            "raw_observation_fingerprint": (
                observation.runtime_artifact_observation.observation_fingerprint
            ),
            "source_cursor_fingerprint": observation.source_cursor_fingerprint,
            "source_device": observation.source_device,
            "source_inode": observation.source_inode,
            "source_log_path": observation.source_log_path,
            "source_prefix_sha256": observation.source_prefix_sha256,
            "source_prefix_size": observation.source_prefix_size,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
