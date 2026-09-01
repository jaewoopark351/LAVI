#20260901_kpopmodder: Seal the exact direct binding state between status and JVM observations.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_RUNTIME_OBSERVATION_BINDING_SEAL = object()


@dataclass(frozen=True, slots=True)
class RuntimeObservationBinding:
    status_observation_fingerprint: str
    artifact_observation_fingerprint: str
    directly_bound: bool
    limitation_reason: str
    session_id: str
    connection_generation: int | None
    binding_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(
            self.status_observation_fingerprint,
            self.artifact_observation_fingerprint,
            self.directly_bound,
            self.limitation_reason,
            self.session_id,
            self.connection_generation,
        )
        if self._seal is not _RUNTIME_OBSERVATION_BINDING_SEAL:
            raise ValueError("runtime observation binding must be created by its binder")
        if self.binding_fingerprint != expected or self._integrity != expected:
            raise ValueError("runtime observation binding integrity mismatch")


def _create_runtime_observation_binding(
    *,
    status_observation_fingerprint: str,
    artifact_observation_fingerprint: str,
    directly_bound: bool,
    limitation_reason: str,
    session_id: str,
    connection_generation: int | None,
) -> RuntimeObservationBinding:
    fingerprint = _fingerprint(
        status_observation_fingerprint,
        artifact_observation_fingerprint,
        directly_bound,
        limitation_reason,
        session_id,
        connection_generation,
    )
    return RuntimeObservationBinding(
        status_observation_fingerprint=status_observation_fingerprint,
        artifact_observation_fingerprint=artifact_observation_fingerprint,
        directly_bound=directly_bound,
        limitation_reason=limitation_reason,
        session_id=session_id,
        connection_generation=connection_generation,
        binding_fingerprint=fingerprint,
        _seal=_RUNTIME_OBSERVATION_BINDING_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(
    status_fingerprint: str,
    artifact_fingerprint: str,
    directly_bound: bool,
    limitation_reason: str,
    session_id: str,
    connection_generation: int | None,
) -> str:
    payload = json.dumps(
        {
            "artifact_observation_fingerprint": artifact_fingerprint,
            "connection_generation": connection_generation,
            "directly_bound": directly_bound,
            "limitation_reason": limitation_reason,
            "session_id": session_id,
            "status_observation_fingerprint": status_fingerprint,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
