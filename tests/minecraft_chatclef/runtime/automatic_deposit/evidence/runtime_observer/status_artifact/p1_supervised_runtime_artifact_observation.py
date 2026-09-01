#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_P1_SUPERVISED_RUNTIME_ARTIFACT_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1SupervisedRuntimeArtifactObservation:
    backend_id: str
    loader_id: str
    minecraft_version: str
    chatclef_version: str
    loaded_jar_path: str
    loaded_jar_size: int
    loaded_jar_sha256: str
    sha256_evidence_source: str
    minecraft_process_id: int
    session_id: str
    connection_generation: int
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(self)
        if self._seal is not _P1_SUPERVISED_RUNTIME_ARTIFACT_SEAL:
            raise ValueError("supervised runtime artifact must be observer-created")
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("supervised runtime artifact integrity mismatch")


def _create_p1_supervised_runtime_artifact_observation(
    **values: object,
) -> P1SupervisedRuntimeArtifactObservation:
    provisional = P1SupervisedRuntimeArtifactObservation.__new__(
        P1SupervisedRuntimeArtifactObservation
    )
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return P1SupervisedRuntimeArtifactObservation(
        **values,
        observation_fingerprint=fingerprint,
        _seal=_P1_SUPERVISED_RUNTIME_ARTIFACT_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(observation: P1SupervisedRuntimeArtifactObservation) -> str:
    payload = json.dumps(
        {
            name: getattr(observation, name)
            for name in (
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
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
