#20260831_kpopmodder: Seal Carry On installed-or-absent evidence derived by collectors.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from .carry_on_identity import AutomaticDepositCarryOnIdentity


_CARRY_ON_IDENTITY_COLLECTION_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositCarryOnIdentityCollection:
    ok: bool
    reason: str
    identity: AutomaticDepositCarryOnIdentity | None = None
    #20260901_kpopmodder: Bind Carry On evidence to the exact runtime artifact snapshot.
    runtime_snapshot_fingerprint: str = ""
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _CARRY_ON_IDENTITY_COLLECTION_SEAL:
            raise ValueError("Carry On identity collection must be created by its collector")
        if self._integrity != _collection_integrity(
            self.ok,
            self.reason,
            self.identity,
            self.runtime_snapshot_fingerprint,
        ):
            raise ValueError("Carry On identity collection integrity mismatch")


def _create_carry_on_identity_collection(
    ok: bool,
    reason: str,
    identity: AutomaticDepositCarryOnIdentity | None = None,
    *,
    runtime_snapshot_fingerprint: str = "",
) -> AutomaticDepositCarryOnIdentityCollection:
    return AutomaticDepositCarryOnIdentityCollection(
        ok=ok,
        reason=reason,
        identity=identity,
        runtime_snapshot_fingerprint=runtime_snapshot_fingerprint,
        _seal=_CARRY_ON_IDENTITY_COLLECTION_SEAL,
        _integrity=_collection_integrity(
            ok,
            reason,
            identity,
            runtime_snapshot_fingerprint,
        ),
    )


def _collection_integrity(
    ok: bool,
    reason: str,
    identity: AutomaticDepositCarryOnIdentity | None,
    runtime_snapshot_fingerprint: str,
) -> str:
    payload = {
        "identity": (
            None
            if identity is None
            else {
                "config_fingerprint": identity.config_fingerprint,
                "jar_path": identity.jar_path,
                "jar_sha256": identity.jar_sha256,
                "loaded": identity.loaded,
                "loader_mod_list_fingerprint": (
                    identity.loader_mod_list_fingerprint
                ),
                "mods_directory_fingerprint": identity.mods_directory_fingerprint,
                "observation_reason": identity.observation_reason,
                "presence": identity.presence.value,
                "version": identity.version,
            }
        ),
        "ok": ok,
        "reason": reason,
        "runtime_snapshot_fingerprint": runtime_snapshot_fingerprint,
    }
    encoded = json.dumps(
        payload,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()
