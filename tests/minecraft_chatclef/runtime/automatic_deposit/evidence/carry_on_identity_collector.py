#20260831_kpopmodder: Derive Carry On installed or absent identity from loader and disk evidence.
from __future__ import annotations

from pathlib import Path

from .carry_on_configuration_snapshot import (
    AutomaticDepositCarryOnConfigurationSnapshot,
)
from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
    _create_carry_on_identity_collection,
)
from .carry_on_presence import AutomaticDepositCarryOnPresence
from .mods_directory_snapshot import AutomaticDepositModsDirectorySnapshot
from .runtime_artifact_snapshot import AutomaticDepositRuntimeArtifactSnapshot


def collect_automatic_deposit_carry_on_identity(
    mods_snapshot: AutomaticDepositModsDirectorySnapshot,
    runtime_snapshot: AutomaticDepositRuntimeArtifactSnapshot,
    *,
    configuration_snapshot: AutomaticDepositCarryOnConfigurationSnapshot | None = None,
) -> AutomaticDepositCarryOnIdentityCollection:
    if not isinstance(mods_snapshot, AutomaticDepositModsDirectorySnapshot):
        return _failure("CARRY_ON_MODS_SNAPSHOT_NOT_TYPED")
    if not isinstance(runtime_snapshot, AutomaticDepositRuntimeArtifactSnapshot):
        return _failure("CARRY_ON_RUNTIME_SNAPSHOT_NOT_TYPED")
    runtime_matches = tuple(
        mod for mod in runtime_snapshot.loaded_mods if mod.mod_id == "carryon"
    )
    disk_matches = tuple(
        artifact
        for artifact in mods_snapshot.artifacts
        if artifact.metadata_status == "COMPLETE" and artifact.mod_id == "carryon"
    )
    unclassified = tuple(
        artifact
        for artifact in mods_snapshot.artifacts
        if artifact.metadata_status != "COMPLETE"
    )
    if not runtime_matches and not disk_matches:
        if unclassified:
            return _failure("CARRY_ON_ABSENCE_BLOCKED_BY_UNCLASSIFIED_JAR")
        identity = AutomaticDepositCarryOnIdentity(
            presence=AutomaticDepositCarryOnPresence.ABSENT,
            loaded=False,
            loader_mod_list_fingerprint=(
                runtime_snapshot.loader_mod_list_fingerprint
            ),
            mods_directory_fingerprint=mods_snapshot.mods_directory_fingerprint,
            observation_reason=(
                "runtime loader list and stable mods scan contain no carryon mod"
            ),
        )
        return _create_carry_on_identity_collection(
            True,
            "CARRY_ON_ABSENCE_COLLECTED",
            identity,
            runtime_snapshot_fingerprint=runtime_snapshot.snapshot_fingerprint,
        )
    if len(runtime_matches) != 1 or len(disk_matches) != 1:
        return _failure("CARRY_ON_LOADER_AND_DISK_IDENTITY_NOT_EXCLUSIVE")
    runtime_mod = runtime_matches[0]
    disk_mod = disk_matches[0]
    if _canonical(runtime_mod.code_source) != _canonical(disk_mod.path):
        return _failure("CARRY_ON_RUNTIME_CODE_SOURCE_MISMATCH")
    if runtime_mod.version != disk_mod.version:
        return _failure("CARRY_ON_RUNTIME_AND_DISK_VERSION_MISMATCH")
    configuration_reason = ""
    configuration_fingerprint = ""
    if not isinstance(
        configuration_snapshot,
        AutomaticDepositCarryOnConfigurationSnapshot,
    ):
        configuration_reason = "CARRY_ON_CONFIGURATION_SNAPSHOT_NOT_TYPED"
    elif _canonical(configuration_snapshot.instance_root) != _canonical(
        mods_snapshot.instance_root
    ):
        configuration_reason = "CARRY_ON_CONFIG_INSTANCE_MISMATCH"
    else:
        configuration_fingerprint = configuration_snapshot.fingerprint
    identity = AutomaticDepositCarryOnIdentity(
        presence=AutomaticDepositCarryOnPresence.INSTALLED,
        loaded=True,
        jar_path=disk_mod.path,
        jar_sha256=disk_mod.sha256,
        version=disk_mod.version,
        config_fingerprint=configuration_fingerprint,
        loader_mod_list_fingerprint=runtime_snapshot.loader_mod_list_fingerprint,
        mods_directory_fingerprint=mods_snapshot.mods_directory_fingerprint,
        observation_reason=(
            "runtime loader identity matches stable carryon JAR; "
            + (
                "exact configuration proof collected"
                if not configuration_reason
                else f"configuration proof unavailable: {configuration_reason}"
            )
        ),
    )
    #20260901_kpopmodder: Preserve installed artifact fact without promoting missing configuration proof.
    if configuration_reason:
        return _create_carry_on_identity_collection(
            False,
            configuration_reason,
            identity,
            runtime_snapshot_fingerprint=runtime_snapshot.snapshot_fingerprint,
        )
    return _create_carry_on_identity_collection(
        True,
        "CARRY_ON_INSTALLED_IDENTITY_COLLECTED",
        identity,
        runtime_snapshot_fingerprint=runtime_snapshot.snapshot_fingerprint,
    )


def _failure(reason: str) -> AutomaticDepositCarryOnIdentityCollection:
    return _create_carry_on_identity_collection(False, reason)


def _canonical(value: str) -> str:
    return str(Path(value).resolve(strict=False)).casefold()
