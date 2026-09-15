#20260915_kpopmodder: Own immutable Python FIND vocabulary and read-only local Minecraft language JSON loading.
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
from types import MappingProxyType
import unicodedata
from typing import Iterable


class FindNameRepository:
    """Names are vocabulary, not a registry whitelist. Java remains the live ID authority.

    A bundled entity vocabulary and the existing official item-name artifact provide
    an offline baseline. Minecraft's installed ko_kr asset supplies the full local
    language vocabulary, including items outside TaskCatalogue. No network, world,
    instance configuration, or asset files are written by this reader.
    """

    _LIMIT = 4 * 1024 * 1024
    _ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)
    _LANG = re.compile(r"(entity|block|item)\.minecraft\.([a-z0-9_]+)\Z", re.ASCII)

    def __init__(self, resource_dir: Path | None = None, *, asset_index_paths: Iterable[Path] | None = None):
        directory = resource_dir or Path(__file__).resolve().parents[2] / "resources"
        self.resource_dir = directory
        aliases, alias_hash = self._read_json(directory / "korean_find_aliases.json")
        if (aliases.get("schema_version") != 1 or type(aliases.get("schema_version")) is not int
                or aliases.get("minecraft_version") != "1.20.1"
                or set(aliases) != {"schema_version", "minecraft_version", "asset_index_paths", "entity", "block", "item"}):
            raise ValueError("find_invalid_alias_schema")
        configured = aliases["asset_index_paths"]
        if type(configured) is not list or len(configured) > 8 or any(type(p) is not str or not p for p in configured):
            raise ValueError("find_invalid_asset_paths")
        entity_data, entity_hash = self._read_json(directory / "korean_find_entity_names.json")
        if (type(entity_data.get("schema_version")) is not int or entity_data.get("schema_version") != 1
                or entity_data.get("minecraft_version") != "1.20.1" or type(entity_data.get("entity")) is not dict):
            raise ValueError("find_invalid_entity_names")
        names: dict[tuple[str, str], str] = {}
        for identifier, label in entity_data["entity"].items():
            self._validate_entry(identifier, label)
            names[("entity", identifier)] = label
        # Reuse NAME DATA only. Never use GET's catalog as FIND admission policy.
        policy, fallback_hash = self._read_json(directory / "chatclef_item_command_target_policy.json")
        for row in policy["targets"].values():
            match = self._LANG.fullmatch(str(row.get("lang_key", "")))
            if match:
                kind, path = match.groups()
                label = row["display_name"]
                self._validate_entry("minecraft:" + path, label)
                names[(kind, "minecraft:" + path)] = label
        paths = tuple(asset_index_paths) if asset_index_paths is not None else (
            tuple(Path(os.path.expandvars(p)).expanduser() for p in configured) or self._default_asset_paths()
        )
        language, language_hash, asset_status, asset_warnings = self._read_language(paths)
        for key, label in language.items():
            match = self._LANG.fullmatch(key)
            if not match or not self._valid_label(label):
                continue
            kind, path = match.groups()
            identifier = "minecraft:" + path
            # Locale JSON also includes old keys and display-only variants, not registry entries.
            if kind == "entity" and identifier not in entity_data["entity"]:
                continue
            if path.endswith("_pottery_shard") or (kind == "item" and path == "lodestone_compass"):
                continue
            if kind == "block" and path in {"bed", "banner"}:
                continue
            names[(kind, identifier)] = label
        # A BlockItem generally uses the block translation key. This is a vocabulary
        # candidate only: e.g. dropped water cannot pass Java's actual ITEM registry.
        block_items = set()
        for (kind, identifier), label in tuple(names.items()):
            if kind == "block" and ("item", identifier) not in names:
                names[("item", identifier)] = label
                block_items.add(identifier)
        extra: dict[tuple[str, str], tuple[str, ...]] = {}
        for kind in ("entity", "block", "item"):
            section = aliases[kind]
            if type(section) is not dict or len(section) > 20000:
                raise ValueError("find_invalid_alias_section")
            for identifier, values in section.items():
                if type(values) is not list or not 1 <= len(values) <= 64:
                    raise ValueError("find_invalid_alias_values")
                for label in values:
                    self._validate_entry(identifier, label)
                extra[(kind, identifier)] = tuple(values)
                names.setdefault((kind, identifier), values[0])
        self.names = MappingProxyType(names)
        self.aliases = MappingProxyType(extra)
        self.block_items = frozenset(block_items)
        digest = hashlib.sha256((alias_hash + entity_hash + fallback_hash + language_hash).encode("ascii")).hexdigest()
        self.diagnostics = MappingProxyType({
            "vocabulary_sha256": digest, "asset_status": asset_status,
            "asset_warnings": asset_warnings, "name_count": len(names),
            "alias_count": sum(map(len, extra.values())), "minecraft_version": "1.20.1",
        })

    @classmethod
    def _valid_label(cls, label: object) -> bool:
        return (type(label) is str and 1 <= len(label) <= 128 and "%" not in label
                and label.strip() == label and bool(label.strip())
                and all(unicodedata.category(c)[0] != "C" for c in label))

    @classmethod
    def _validate_entry(cls, identifier, label):
        if type(identifier) is not str or len(identifier) > 128 or not cls._ID.fullmatch(identifier) or not cls._valid_label(label):
            raise ValueError("find_invalid_name_entry")

    @staticmethod
    def _unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("find_duplicate_json_key")
            result[key] = value
        return result

    @classmethod
    def _read_json(cls, path):
        with Path(path).open("rb") as handle:
            raw = handle.read(cls._LIMIT + 1)
        if len(raw) > cls._LIMIT:
            raise ValueError("find_json_budget_exceeded")
        value = json.loads(raw.decode("utf-8-sig"), object_pairs_hook=cls._unique_object)
        if type(value) is not dict:
            raise ValueError("find_json_object_required")
        return value, hashlib.sha256(raw).hexdigest()

    @staticmethod
    def _default_asset_paths():
        home = Path.home()
        result = [home / "curseforge/minecraft/Install/assets/indexes/5.json"]
        appdata = os.environ.get("APPDATA")
        if appdata:
            result.append(Path(appdata) / ".minecraft/assets/indexes/5.json")
        else:
            result.append(home / ".minecraft/assets/indexes/5.json")
        return tuple(result)

    @classmethod
    def _read_language(cls, paths):
        warnings = 0
        for path in paths:
            path = Path(path)
            try:
                if not path.is_file():
                    continue
                index, _ = cls._read_json(path)
                entry = index["objects"]["minecraft/lang/ko_kr.json"]
                sha1 = entry["hash"]
                if type(sha1) is not str or not re.fullmatch(r"[0-9a-f]{40}", sha1):
                    raise ValueError("find_invalid_asset_hash")
                asset = path.parent.parent / "objects" / sha1[:2] / sha1
                with asset.open("rb") as handle:
                    raw = handle.read(cls._LIMIT + 1)
                if len(raw) > cls._LIMIT or hashlib.sha1(raw).hexdigest() != sha1:
                    raise ValueError("find_asset_integrity_failed")
                language = json.loads(raw.decode("utf-8"), object_pairs_hook=cls._unique_object)
                if type(language) is not dict:
                    raise ValueError("find_invalid_language_asset")
                return language, hashlib.sha256(raw).hexdigest(), "local_asset_loaded", warnings
            except (OSError, UnicodeError, ValueError, KeyError, TypeError):
                warnings += 1
        return {}, "", "bundled_names_only", warnings
