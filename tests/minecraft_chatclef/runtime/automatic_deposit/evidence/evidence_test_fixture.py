#20260831_kpopmodder: Provide in-memory read-only file evidence for hermetic collectors.
from __future__ import annotations

import hashlib
import io
import json
import stat
import zipfile
from pathlib import Path
from types import SimpleNamespace


class HermeticEvidenceFileSystem:
    def __init__(self, instance_root: Path, mods_directory: Path) -> None:
        self.instance_root = instance_root.resolve(strict=False)
        self.mods_directory = mods_directory.resolve(strict=False)
        self._payloads: dict[str, bytes] = {}
        self._directory_entries: list[Path] = []
        self._unstable_paths: set[str] = set()
        self._stat_calls: dict[str, int] = {}

    def add_file(self, path: Path, payload: bytes, *, in_mods: bool = False) -> Path:
        canonical = path.resolve(strict=False)
        self._payloads[_key(canonical)] = payload
        if in_mods:
            self._directory_entries.append(canonical)
        return canonical

    def make_unstable(self, path: Path) -> None:
        self._unstable_paths.add(_key(path.resolve(strict=False)))

    def read_directory(self, path: Path) -> tuple[Path, ...]:
        if _key(path) != _key(self.mods_directory):
            raise FileNotFoundError(path)
        return tuple(self._directory_entries)

    def read_bytes(self, path: Path) -> bytes:
        return self._payloads[_key(path)]

    def read_stat(self, path: Path) -> object:
        canonical = path.resolve(strict=False)
        key = _key(canonical)
        call = self._stat_calls.get(key, 0)
        self._stat_calls[key] = call + 1
        if key == _key(self.mods_directory):
            return SimpleNamespace(
                st_dev=1,
                st_ino=2,
                st_mode=stat.S_IFDIR | 0o755,
                st_size=0,
                st_mtime_ns=100,
            )
        payload = self._payloads[key]
        mtime = 201 if key in self._unstable_paths and call > 0 else 200
        return SimpleNamespace(
            st_dev=1,
            st_ino=_stable_inode(key),
            st_mode=stat.S_IFREG | 0o644,
            st_size=len(payload),
            st_mtime_ns=mtime,
        )


def fabric_mod_jar(mod_id: str, version: str) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
        metadata_entry = zipfile.ZipInfo(
            "fabric.mod.json",
            date_time=(1980, 1, 1, 0, 0, 0),
        )
        metadata_entry.compress_type = zipfile.ZIP_STORED
        archive.writestr(
            metadata_entry,
            json.dumps(
                {"schemaVersion": 1, "id": mod_id, "version": version},
                separators=(",", ":"),
                sort_keys=True,
            ).encode("utf-8"),
        )
    return output.getvalue()


def runtime_status(
    chatclef_path: Path,
    *,
    chatclef_version: str = "1.20.1-0.18.23",
    carry_on_path: Path | None = None,
    carry_on_version: str = "2.1.2.7",
    artifact_extra: dict[str, object] | None = None,
) -> dict[str, object]:
    mods = [
        {
            "id": "altoclef",
            "version": chatclef_version,
            "code_source": str(chatclef_path.resolve(strict=False)),
        }
    ]
    if carry_on_path is not None:
        mods.append(
            {
                "id": "carryon",
                "version": carry_on_version,
                "code_source": str(carry_on_path.resolve(strict=False)),
            }
        )
    artifact: dict[str, object] = {
        "schema_version": "automatic-deposit-runtime-artifact/v1",
        "loaded_code_source": str(chatclef_path.resolve(strict=False)),
        "loaded_mods": mods,
    }
    artifact.update(artifact_extra or {})
    return {
        "backend_id": "fabric_chatclef",
        "details": {"runtime_artifact": artifact},
    }


def _key(path: Path) -> str:
    return str(path.resolve(strict=False)).casefold()


def _stable_inode(key: str) -> int:
    digest = hashlib.sha256(key.encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") % 100_000 + 10
