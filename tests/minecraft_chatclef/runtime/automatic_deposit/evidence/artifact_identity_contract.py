#20260831_kpopmodder: Require one deployed JAR whose bytes match source and loaded code.
from __future__ import annotations

import re
from pathlib import Path

from .artifact_identity import AutomaticDepositArtifactIdentity
from .artifact_identity_verification import (
    AutomaticDepositArtifactIdentityVerification,
)


def verify_automatic_deposit_artifact_identity(
    identity: object,
) -> AutomaticDepositArtifactIdentityVerification:
    #20260901_kpopmodder: Reject malformed nested identity without escaping the evidence gate.
    if not isinstance(identity, AutomaticDepositArtifactIdentity):
        return AutomaticDepositArtifactIdentityVerification(
            ok=False,
            reason="ARTIFACT_IDENTITY_INCONCLUSIVE",
            errors=("ARTIFACT_IDENTITY_NOT_TYPED",),
        )
    errors: list[str] = []
    source_path = _canonical(identity.source_jar_path)
    deployed_path = _canonical(identity.deployed_jar_path)
    loaded_path = _canonical(identity.loaded_code_source)
    discovered = tuple(_canonical(path) for path in identity.discovered_chatclef_jars)
    if not Path(identity.source_jar_path).is_absolute():
        errors.append("SOURCE_JAR_PATH_NOT_ABSOLUTE")
    if not Path(identity.deployed_jar_path).is_absolute():
        errors.append("DEPLOYED_JAR_PATH_NOT_ABSOLUTE")
    if not Path(identity.loaded_code_source).is_absolute():
        errors.append("LOADED_CODE_SOURCE_NOT_ABSOLUTE")
    if not _is_sha256(identity.source_jar_sha256):
        errors.append("SOURCE_JAR_SHA256_INVALID")
    if not _is_sha256(identity.deployed_jar_sha256):
        errors.append("DEPLOYED_JAR_SHA256_INVALID")
    if identity.source_jar_sha256.lower() != identity.deployed_jar_sha256.lower():
        errors.append("SOURCE_AND_DEPLOYED_JAR_SHA256_DIFFER")
    if loaded_path != deployed_path:
        errors.append("LOADED_CODE_SOURCE_IS_NOT_DEPLOYED_JAR")
    if len(discovered) != 1 or discovered[0] != deployed_path:
        errors.append("DEPLOYED_CHATCLEF_JAR_NOT_EXCLUSIVE")
    if source_path == deployed_path:
        errors.append("SOURCE_AND_DEPLOYED_PATHS_NOT_DISTINCT")
    return AutomaticDepositArtifactIdentityVerification(
        ok=not errors,
        reason=(
            "ARTIFACT_IDENTITY_VERIFIED"
            if not errors
            else "ARTIFACT_IDENTITY_INCONCLUSIVE"
        ),
        errors=tuple(errors),
    )


def _canonical(value: str) -> str:
    return str(Path(value).resolve(strict=False)).casefold()


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(value or "")))
