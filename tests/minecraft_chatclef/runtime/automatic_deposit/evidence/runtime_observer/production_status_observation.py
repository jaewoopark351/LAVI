#20260901_kpopmodder: Seal one canonical connected and idle Fabric production status observation.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_PRODUCTION_STATUS_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class ProductionFabricStatusObservation:
    backend_id: str
    lifecycle_state: str
    session_id: str
    session_count: int
    connection_generation: int
    active_request_id: str | None
    protocol_version: int
    session_backend: str
    session_loader: str
    session_phase: str
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(
            self.backend_id,
            self.lifecycle_state,
            self.session_id,
            self.session_count,
            self.connection_generation,
            self.active_request_id,
            self.protocol_version,
            self.session_backend,
            self.session_loader,
            self.session_phase,
        )
        if self._seal is not _PRODUCTION_STATUS_OBSERVATION_SEAL:
            raise ValueError("production status observation must be created by its observer")
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("production status observation integrity mismatch")


def _create_production_status_observation(
    *,
    backend_id: str,
    lifecycle_state: str,
    session_id: str,
    session_count: int,
    connection_generation: int,
    active_request_id: str | None,
    protocol_version: int,
    session_backend: str,
    session_loader: str,
    session_phase: str,
) -> ProductionFabricStatusObservation:
    fingerprint = _fingerprint(
        backend_id,
        lifecycle_state,
        session_id,
        session_count,
        connection_generation,
        active_request_id,
        protocol_version,
        session_backend,
        session_loader,
        session_phase,
    )
    return ProductionFabricStatusObservation(
        backend_id=backend_id,
        lifecycle_state=lifecycle_state,
        session_id=session_id,
        session_count=session_count,
        connection_generation=connection_generation,
        active_request_id=active_request_id,
        protocol_version=protocol_version,
        session_backend=session_backend,
        session_loader=session_loader,
        session_phase=session_phase,
        observation_fingerprint=fingerprint,
        _seal=_PRODUCTION_STATUS_OBSERVATION_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(
    backend_id: str,
    lifecycle_state: str,
    session_id: str,
    session_count: int,
    connection_generation: int,
    active_request_id: str | None,
    protocol_version: int,
    session_backend: str,
    session_loader: str,
    session_phase: str,
) -> str:
    payload = json.dumps(
        {
            "active_request_id": active_request_id,
            "backend_id": backend_id,
            "connection_generation": connection_generation,
            "lifecycle_state": lifecycle_state,
            "protocol_version": protocol_version,
            "session_backend": session_backend,
            "session_count": session_count,
            "session_id": session_id,
            "session_loader": session_loader,
            "session_phase": session_phase,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
