#20260831_kpopmodder: Return artifact collection failures without fabricating identity.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from .artifact_identity import AutomaticDepositArtifactIdentity


_ARTIFACT_COLLECTION_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositArtifactIdentityCollection:
    ok: bool
    reason: str
    identity: AutomaticDepositArtifactIdentity | None = None
    runtime_snapshot_fingerprint: str = ""
    mods_directory_fingerprint: str = ""
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _ARTIFACT_COLLECTION_SEAL:
            raise ValueError("artifact collection must be created by its collector")
        if self._integrity != _collection_integrity(
            self.ok,
            self.reason,
            self.identity,
            self.runtime_snapshot_fingerprint,
            self.mods_directory_fingerprint,
        ):
            raise ValueError("artifact collection integrity mismatch")


def _create_artifact_identity_collection(
    ok: bool,
    reason: str,
    identity: AutomaticDepositArtifactIdentity | None = None,
    *,
    runtime_snapshot_fingerprint: str = "",
    mods_directory_fingerprint: str = "",
) -> AutomaticDepositArtifactIdentityCollection:
    return AutomaticDepositArtifactIdentityCollection(
        ok=ok,
        reason=reason,
        identity=identity,
        runtime_snapshot_fingerprint=runtime_snapshot_fingerprint,
        mods_directory_fingerprint=mods_directory_fingerprint,
        _seal=_ARTIFACT_COLLECTION_SEAL,
        _integrity=_collection_integrity(
            ok,
            reason,
            identity,
            runtime_snapshot_fingerprint,
            mods_directory_fingerprint,
        ),
    )


def _collection_integrity(
    ok: bool,
    reason: str,
    identity: AutomaticDepositArtifactIdentity | None,
    runtime_snapshot_fingerprint: str,
    mods_directory_fingerprint: str,
) -> str:
    payload = {
        "ok": ok,
        "reason": reason,
        "runtime_snapshot_fingerprint": runtime_snapshot_fingerprint,
        "mods_directory_fingerprint": mods_directory_fingerprint,
        "identity": (
            None
            if identity is None
            else {
                "source_jar_path": identity.source_jar_path,
                "source_jar_sha256": identity.source_jar_sha256,
                "deployed_jar_path": identity.deployed_jar_path,
                "deployed_jar_sha256": identity.deployed_jar_sha256,
                "loaded_code_source": identity.loaded_code_source,
                "discovered_chatclef_jars": identity.discovered_chatclef_jars,
            }
        ),
    }
    encoded = json.dumps(
        payload,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()
