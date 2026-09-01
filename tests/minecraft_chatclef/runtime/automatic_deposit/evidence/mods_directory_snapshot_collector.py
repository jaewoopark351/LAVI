#20260831_kpopmodder: Discover Fabric mod identities from stable JAR bytes in one validated mods directory.
from __future__ import annotations

import io
import json
import re
import stat
import zipfile
from collections.abc import Callable, Iterable, Mapping
from pathlib import Path

from ...preflight.shared_file_reader import read_shared_bytes
from .mods_directory_snapshot import (
    AutomaticDepositModArtifact,
    AutomaticDepositModsDirectorySnapshot,
    _create_mods_directory_snapshot,
)
from .stable_file_digest import read_automatic_deposit_stable_file


_MAX_MOD_JARS = 512
_MAX_FABRIC_MOD_JSON_BYTES = 1024 * 1024


def collect_automatic_deposit_mods_directory_snapshot(
    instance_root: object,
    mods_directory: object,
    *,
    directory_reader: Callable[[Path], Iterable[Path]] | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[AutomaticDepositModsDirectorySnapshot | None, str]:
    instance_path = _absolute_path(instance_root)
    mods_path = _absolute_path(mods_directory)
    if instance_path is None or mods_path is None:
        return None, "MODS_DIRECTORY_PATH_INVALID"
    if not _is_within(mods_path, instance_path):
        return None, "MODS_DIRECTORY_OUTSIDE_INSTANCE"
    read_directory = directory_reader or (lambda candidate: candidate.iterdir())
    read_stat = stat_reader or (lambda candidate: candidate.stat())
    read_bytes = bytes_reader or read_shared_bytes
    try:
        before = read_stat(mods_path)
        before_identity = _directory_identity(before)
        if not stat.S_ISDIR(int(getattr(before, "st_mode"))):
            return None, "MODS_DIRECTORY_NOT_DIRECTORY"
        entries = tuple(read_directory(mods_path))
    except Exception as error:
        return None, f"MODS_DIRECTORY_SCAN_FAILED:{type(error).__name__}"
    jar_paths: list[Path] = []
    seen: set[str] = set()
    for raw_entry in entries:
        if not isinstance(raw_entry, Path):
            return None, "MODS_DIRECTORY_ENTRY_NOT_PATH"
        entry = raw_entry.resolve(strict=False)
        if not _is_direct_child(entry, mods_path):
            return None, "MODS_DIRECTORY_ENTRY_OUTSIDE_DIRECTORY"
        if entry.suffix.casefold() != ".jar":
            continue
        canonical = str(entry).casefold()
        if canonical in seen:
            return None, "MODS_DIRECTORY_ENTRY_DUPLICATE"
        seen.add(canonical)
        jar_paths.append(entry)
    if len(jar_paths) > _MAX_MOD_JARS:
        return None, "MODS_DIRECTORY_JAR_COUNT_OUT_OF_BOUND"
    artifacts: list[AutomaticDepositModArtifact] = []
    for jar_path in sorted(jar_paths, key=lambda value: str(value).casefold()):
        digest, payload, reason = read_automatic_deposit_stable_file(
            jar_path,
            bytes_reader=read_bytes,
            stat_reader=read_stat,
        )
        if digest is None or payload is None:
            return None, f"MOD_JAR_{reason}"
        mod_id, version, metadata_status = _fabric_mod_identity(payload)
        artifacts.append(
            AutomaticDepositModArtifact(
                path=digest.path,
                sha256=digest.sha256,
                size=digest.size,
                mtime_ns=digest.mtime_ns,
                mod_id=mod_id,
                version=version,
                metadata_status=metadata_status,
            )
        )
    try:
        after = read_stat(mods_path)
        after_identity = _directory_identity(after)
    except Exception as error:
        return None, f"MODS_DIRECTORY_RECHECK_FAILED:{type(error).__name__}"
    if before_identity != after_identity:
        return None, "MODS_DIRECTORY_CHANGED_DURING_SCAN"
    return (
        _create_mods_directory_snapshot(
            str(instance_path),
            str(mods_path),
            tuple(artifacts),
        ),
        "MODS_DIRECTORY_SNAPSHOT_COLLECTED",
    )


def _fabric_mod_identity(payload: bytes) -> tuple[str, str, str]:
    try:
        with zipfile.ZipFile(io.BytesIO(payload), "r") as archive:
            entries = archive.infolist()
            if len(entries) > 100_000:
                return "", "", "INVALID"
            matches = tuple(
                info for info in entries if info.filename == "fabric.mod.json"
            )
            if len(matches) != 1:
                return "", "", "MISSING" if not matches else "INVALID"
            info = matches[0]
            if info.file_size < 1 or info.file_size > _MAX_FABRIC_MOD_JSON_BYTES:
                return "", "", "INVALID"
            with archive.open(info, "r") as metadata_stream:
                raw_metadata = metadata_stream.read(_MAX_FABRIC_MOD_JSON_BYTES + 1)
                if (
                    len(raw_metadata) > _MAX_FABRIC_MOD_JSON_BYTES
                    or metadata_stream.read(1)
                ):
                    return "", "", "INVALID"
        metadata = json.loads(raw_metadata.decode("utf-8", errors="strict"))
    except (OSError, UnicodeError, ValueError, zipfile.BadZipFile, KeyError):
        return "", "", "INVALID"
    if not isinstance(metadata, Mapping):
        return "", "", "INVALID"
    mod_id = str(metadata.get("id") or "").strip().casefold()
    version = str(metadata.get("version") or "").strip()
    if (
        not re.fullmatch(r"[a-z0-9][a-z0-9._-]{0,127}", mod_id)
        or not version
        or len(version) > 128
    ):
        return "", "", "INVALID"
    return mod_id, version, "COMPLETE"


def _absolute_path(value: object) -> Path | None:
    text = str(value or "").strip()
    if not text:
        return None
    path = Path(text)
    return path.resolve(strict=False) if path.is_absolute() else None


def _is_within(candidate: Path, parent: Path) -> bool:
    try:
        candidate.relative_to(parent)
    except ValueError:
        return False
    return candidate != parent


def _is_direct_child(candidate: Path, parent: Path) -> bool:
    return candidate.parent == parent


def _directory_identity(value: object) -> tuple[int, int, int, int]:
    return (
        int(getattr(value, "st_dev")),
        int(getattr(value, "st_ino")),
        int(getattr(value, "st_mode")),
        int(getattr(value, "st_mtime_ns")),
    )
