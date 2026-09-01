#20260831_kpopmodder: Seal runtime-loaded mod and code-source evidence after strict parsing.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_RUNTIME_ARTIFACT_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositRuntimeLoadedMod:
    mod_id: str
    version: str
    code_source: str


@dataclass(frozen=True, slots=True)
class AutomaticDepositRuntimeArtifactSnapshot:
    backend_id: str
    loaded_code_source: str
    loaded_mods: tuple[AutomaticDepositRuntimeLoadedMod, ...]
    loader_mod_list_fingerprint: str
    snapshot_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _RUNTIME_ARTIFACT_SNAPSHOT_SEAL:
            raise ValueError("runtime artifact snapshot must be created by its collector")
        expected_loader = _loader_mod_list_fingerprint(self.loaded_mods)
        expected_snapshot = _runtime_snapshot_fingerprint(
            self.backend_id,
            self.loaded_code_source,
            self.loaded_mods,
        )
        if self.loader_mod_list_fingerprint != expected_loader:
            raise ValueError("runtime loader mod list fingerprint mismatch")
        if self.snapshot_fingerprint != expected_snapshot:
            raise ValueError("runtime artifact snapshot integrity mismatch")
        if self._integrity != expected_snapshot:
            raise ValueError("runtime artifact snapshot seal mismatch")


def _create_runtime_artifact_snapshot(
    backend_id: str,
    loaded_code_source: str,
    loaded_mods: tuple[AutomaticDepositRuntimeLoadedMod, ...],
) -> AutomaticDepositRuntimeArtifactSnapshot:
    loader_fingerprint = _loader_mod_list_fingerprint(loaded_mods)
    snapshot_fingerprint = _runtime_snapshot_fingerprint(
        backend_id,
        loaded_code_source,
        loaded_mods,
    )
    return AutomaticDepositRuntimeArtifactSnapshot(
        backend_id=backend_id,
        loaded_code_source=loaded_code_source,
        loaded_mods=loaded_mods,
        loader_mod_list_fingerprint=loader_fingerprint,
        snapshot_fingerprint=snapshot_fingerprint,
        _seal=_RUNTIME_ARTIFACT_SNAPSHOT_SEAL,
        _integrity=snapshot_fingerprint,
    )


def _loader_mod_list_fingerprint(
    loaded_mods: tuple[AutomaticDepositRuntimeLoadedMod, ...],
) -> str:
    payload = [
        {
            "code_source": mod.code_source,
            "id": mod.mod_id,
            "version": mod.version,
        }
        for mod in loaded_mods
    ]
    return _sha256_json(payload)


def _runtime_snapshot_fingerprint(
    backend_id: str,
    loaded_code_source: str,
    loaded_mods: tuple[AutomaticDepositRuntimeLoadedMod, ...],
) -> str:
    return _sha256_json(
        {
            "backend_id": backend_id,
            "loaded_code_source": loaded_code_source,
            "loaded_mods": [
                {
                    "code_source": mod.code_source,
                    "id": mod.mod_id,
                    "version": mod.version,
                }
                for mod in loaded_mods
            ],
        }
    )


def _sha256_json(value: object) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()
