#20260831_kpopmodder: Parse one exact runtime artifact snapshot from bridge status.
from __future__ import annotations

import re
from collections.abc import Mapping, Sequence
from pathlib import Path

from ...preflight.runtime_bridge_snapshot import runtime_bridge_snapshot
from .runtime_artifact_snapshot import (
    AutomaticDepositRuntimeArtifactSnapshot,
    AutomaticDepositRuntimeLoadedMod,
    _create_runtime_artifact_snapshot,
)


_EXPECTED_BACKEND = "fabric_chatclef"
_EXPECTED_SCHEMA = "automatic-deposit-runtime-artifact/v1"
_ARTIFACT_KEYS = frozenset(("schema_version", "loaded_code_source", "loaded_mods"))
_LOADED_MOD_KEYS = frozenset(("id", "version", "code_source"))


def collect_automatic_deposit_runtime_artifact_snapshot(
    runtime_status: object,
) -> tuple[AutomaticDepositRuntimeArtifactSnapshot | None, str]:
    bridge, bridge_error = runtime_bridge_snapshot(runtime_status)
    if bridge_error:
        return None, f"RUNTIME_ARTIFACT_BRIDGE_INVALID:{bridge_error}"
    if _text(bridge.get("backend_id")) != _EXPECTED_BACKEND:
        return None, "RUNTIME_ARTIFACT_BACKEND_MISMATCH"
    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return None, "RUNTIME_ARTIFACT_DETAILS_MISSING"
    raw_artifact = details.get("runtime_artifact")
    if not isinstance(raw_artifact, Mapping):
        return None, "RUNTIME_ARTIFACT_STATUS_MISSING"
    artifact = dict(raw_artifact)
    if frozenset(artifact) != _ARTIFACT_KEYS:
        return None, "RUNTIME_ARTIFACT_STATUS_SCHEMA_INVALID"
    if artifact.get("schema_version") != _EXPECTED_SCHEMA:
        return None, "RUNTIME_ARTIFACT_STATUS_VERSION_INVALID"
    loaded_code_source = _absolute_path(artifact.get("loaded_code_source"))
    if not loaded_code_source:
        return None, "RUNTIME_LOADED_CODE_SOURCE_INVALID"
    raw_loaded_mods = artifact.get("loaded_mods")
    if (
        not isinstance(raw_loaded_mods, Sequence)
        or isinstance(raw_loaded_mods, (str, bytes, bytearray))
        or len(raw_loaded_mods) < 1
        or len(raw_loaded_mods) > 512
    ):
        return None, "RUNTIME_LOADED_MOD_LIST_INVALID"
    loaded_mods: list[AutomaticDepositRuntimeLoadedMod] = []
    seen_ids: set[str] = set()
    for raw_mod in raw_loaded_mods:
        if not isinstance(raw_mod, Mapping):
            return None, "RUNTIME_LOADED_MOD_ENTRY_INVALID"
        mod = dict(raw_mod)
        if frozenset(mod) != _LOADED_MOD_KEYS:
            return None, "RUNTIME_LOADED_MOD_ENTRY_SCHEMA_INVALID"
        mod_id = _text(mod.get("id")).casefold()
        version = _text(mod.get("version"))
        code_source = _absolute_path(mod.get("code_source"))
        if not _valid_token(mod_id) or not version or len(version) > 128:
            return None, "RUNTIME_LOADED_MOD_IDENTITY_INVALID"
        if not code_source:
            return None, "RUNTIME_LOADED_MOD_CODE_SOURCE_INVALID"
        if mod_id in seen_ids:
            return None, "RUNTIME_LOADED_MOD_ID_DUPLICATE"
        seen_ids.add(mod_id)
        loaded_mods.append(
            AutomaticDepositRuntimeLoadedMod(mod_id, version, code_source)
        )
    loaded_mods.sort(key=lambda mod: mod.mod_id)
    chatclef = tuple(mod for mod in loaded_mods if mod.mod_id == "altoclef")
    if len(chatclef) != 1:
        return None, "RUNTIME_CHATCLEF_MOD_NOT_EXCLUSIVE"
    if _canonical(chatclef[0].code_source) != _canonical(loaded_code_source):
        return None, "RUNTIME_CHATCLEF_CODE_SOURCE_MISMATCH"
    return (
        _create_runtime_artifact_snapshot(
            _EXPECTED_BACKEND,
            loaded_code_source,
            tuple(loaded_mods),
        ),
        "RUNTIME_ARTIFACT_SNAPSHOT_COLLECTED",
    )


def _absolute_path(value: object) -> str:
    text = _text(value)
    if not text or not Path(text).is_absolute():
        return ""
    return str(Path(text).resolve(strict=False))


def _canonical(value: str) -> str:
    return str(Path(value).resolve(strict=False)).casefold()


def _text(value: object) -> str:
    return str(value or "").strip()


def _valid_token(value: str) -> bool:
    return bool(re.fullmatch(r"[a-z0-9][a-z0-9._-]{0,127}", value))
