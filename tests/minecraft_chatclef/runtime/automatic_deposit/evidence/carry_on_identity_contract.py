#20260831_kpopmodder: Validate observable Carry On identity evidence.
from __future__ import annotations

import re
from pathlib import Path

from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_identity_verification import (
    AutomaticDepositCarryOnIdentityVerification,
)
from .carry_on_presence import AutomaticDepositCarryOnPresence


def verify_automatic_deposit_carry_on_identity(
    identity: object,
) -> AutomaticDepositCarryOnIdentityVerification:
    #20260901_kpopmodder: Keep an invalid nested Carry On identity inconclusive instead of raising.
    if not isinstance(identity, AutomaticDepositCarryOnIdentity):
        return AutomaticDepositCarryOnIdentityVerification(
            ok=False,
            reason="CARRY_ON_IDENTITY_INCONCLUSIVE",
            errors=("CARRY_ON_IDENTITY_NOT_TYPED",),
        )
    errors: list[str] = []
    if identity.presence is AutomaticDepositCarryOnPresence.INSTALLED:
        if identity.loaded is not True:
            errors.append("CARRY_ON_INSTALLED_BUT_NOT_LOADED")
        _require_artifact_fields(identity, errors)
        _require_environment_fingerprints(identity, errors)
        if not identity.config_fingerprint:
            errors.append("CARRY_ON_CONFIG_FINGERPRINT_MISSING")
        elif not _is_sha256(identity.config_fingerprint):
            errors.append("CARRY_ON_CONFIG_FINGERPRINT_INVALID")
    elif identity.presence is AutomaticDepositCarryOnPresence.ABSENT:
        if identity.loaded is not False:
            errors.append("CARRY_ON_ABSENT_BUT_LOADED")
        if any(
            (
                identity.jar_path,
                identity.jar_sha256,
                identity.version,
                identity.config_fingerprint,
            )
        ):
            errors.append("CARRY_ON_ABSENCE_HAS_ARTIFACT_FIELDS")
        _require_environment_fingerprints(identity, errors)
        if not identity.observation_reason.strip():
            errors.append("CARRY_ON_ABSENCE_REASON_MISSING")
    elif identity.presence in (
        AutomaticDepositCarryOnPresence.INCOMPATIBLE,
        AutomaticDepositCarryOnPresence.UNREADABLE,
    ):
        if identity.loaded is not True:
            errors.append("CARRY_ON_FAILURE_STATE_NOT_LOADED")
        if not identity.observation_reason.strip():
            errors.append("CARRY_ON_FAILURE_REASON_MISSING")
    else:
        errors.append("CARRY_ON_PRESENCE_INVALID")
    return AutomaticDepositCarryOnIdentityVerification(
        ok=not errors,
        reason=(
            "CARRY_ON_IDENTITY_VERIFIED"
            if not errors
            else "CARRY_ON_IDENTITY_INCONCLUSIVE"
        ),
        errors=tuple(errors),
    )


def _require_artifact_fields(
    identity: AutomaticDepositCarryOnIdentity,
    errors: list[str],
) -> None:
    if not identity.jar_path or not Path(identity.jar_path).is_absolute():
        errors.append("CARRY_ON_JAR_PATH_INVALID")
    if not _is_sha256(identity.jar_sha256):
        errors.append("CARRY_ON_JAR_SHA256_INVALID")
    if not identity.version.strip():
        errors.append("CARRY_ON_VERSION_MISSING")


def _require_environment_fingerprints(
    identity: AutomaticDepositCarryOnIdentity,
    errors: list[str],
) -> None:
    if not _is_sha256(identity.loader_mod_list_fingerprint):
        errors.append("CARRY_ON_LOADER_MOD_LIST_FINGERPRINT_INVALID")
    if not _is_sha256(identity.mods_directory_fingerprint):
        errors.append("CARRY_ON_MODS_DIRECTORY_FINGERPRINT_INVALID")


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(value or "")))
