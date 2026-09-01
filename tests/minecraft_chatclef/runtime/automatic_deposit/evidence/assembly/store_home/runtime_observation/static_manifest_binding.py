# 20260901_kpopmodder: Seal one honest binding classification for an emit-once JVM manifest.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


DIRECT = "DIRECT"
COMMAND_CONTEXT_UNAVAILABLE = "COMMAND_CONTEXT_UNAVAILABLE"
HISTORICAL_SESSION = "HISTORICAL_SESSION"
HISTORICAL_GENERATION = "HISTORICAL_GENERATION"
_CLASSIFICATIONS = frozenset(
    {
        DIRECT,
        COMMAND_CONTEXT_UNAVAILABLE,
        HISTORICAL_SESSION,
        HISTORICAL_GENERATION,
    }
)
_STATIC_MANIFEST_BINDING_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1StaticManifestBinding:
    classification: str
    reason: str
    status_fingerprint: str
    raw_artifact_fingerprint: str
    source_runtime_binding_fingerprint: str
    status_session_id: str
    status_connection_generation: int
    artifact_session_id: str
    artifact_connection_generation: int | None
    binding_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self.classification not in _CLASSIFICATIONS:
            raise ValueError("static manifest binding classification is invalid")
        expected = _fingerprint(self)
        if self._seal is not _STATIC_MANIFEST_BINDING_SEAL:
            raise ValueError("static manifest binding must be created by its classifier")
        if self.binding_fingerprint != expected or self._integrity != expected:
            raise ValueError("static manifest binding integrity mismatch")


def _create_p1_static_manifest_binding(
    *,
    classification: str,
    reason: str,
    status_fingerprint: str,
    raw_artifact_fingerprint: str,
    source_runtime_binding_fingerprint: str,
    status_session_id: str,
    status_connection_generation: int,
    artifact_session_id: str,
    artifact_connection_generation: int | None,
) -> P1StaticManifestBinding:
    provisional = P1StaticManifestBinding.__new__(P1StaticManifestBinding)
    values = {
        "classification": classification,
        "reason": reason,
        "status_fingerprint": status_fingerprint,
        "raw_artifact_fingerprint": raw_artifact_fingerprint,
        "source_runtime_binding_fingerprint": source_runtime_binding_fingerprint,
        "status_session_id": status_session_id,
        "status_connection_generation": status_connection_generation,
        "artifact_session_id": artifact_session_id,
        "artifact_connection_generation": artifact_connection_generation,
    }
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return P1StaticManifestBinding(
        **values,
        binding_fingerprint=fingerprint,
        _seal=_STATIC_MANIFEST_BINDING_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(binding: P1StaticManifestBinding) -> str:
    payload = json.dumps(
        {
            "artifact_connection_generation": (
                binding.artifact_connection_generation
            ),
            "artifact_session_id": binding.artifact_session_id,
            "classification": binding.classification,
            "raw_artifact_fingerprint": binding.raw_artifact_fingerprint,
            "reason": binding.reason,
            "source_runtime_binding_fingerprint": (
                binding.source_runtime_binding_fingerprint
            ),
            "status_connection_generation": (
                binding.status_connection_generation
            ),
            "status_fingerprint": binding.status_fingerprint,
            "status_session_id": binding.status_session_id,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
