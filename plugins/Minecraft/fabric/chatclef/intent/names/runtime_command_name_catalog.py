#20260915_kpopmodder: Index one session-validated registry snapshot without treating language keys as registry IDs.
from __future__ import annotations

from collections.abc import Mapping
import re
from types import MappingProxyType

from ..navigation.find.find_target_resolver import FindTargetResolver


class RuntimeCommandNameCatalog:
    def __init__(self, snapshot: Mapping, vocabulary=None):
        if (not isinstance(snapshot, Mapping) or snapshot.get("schema_version") != 1
                or snapshot.get("minecraft_version") != "1.20.1"):
            raise ValueError("invalid_runtime_catalogue")
        rows = snapshot.get("entries")
        if not isinstance(rows, (list, tuple)) or len(rows) > 100000:
            raise ValueError("invalid_runtime_catalogue_entries")
        self.identity = MappingProxyType({key: snapshot.get(key) for key in ("session_id", "catalogue_sha256")})
        self.butler_user = snapshot.get("butler_user")
        entries, index, token_index = {}, {}, {}
        for row in rows:
            if not isinstance(row, Mapping):
                raise ValueError("invalid_runtime_catalogue_entry")
            kind, identifier = row.get("kind"), row.get("id")
            if kind not in {"item", "block", "entity"} or type(identifier) is not str or not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", identifier):
                raise ValueError("invalid_runtime_catalogue_id")
            key = (kind, identifier)
            if key in entries:
                raise ValueError("duplicate_runtime_catalogue_id")
            tokens, capabilities = row.get("tokens", {}), row.get("capabilities", ())
            if not isinstance(tokens, Mapping) or not isinstance(capabilities, (list, tuple)):
                raise ValueError("invalid_runtime_catalogue_capabilities")
            label = row.get("korean_name")
            if not label and vocabulary is not None:
                label = vocabulary.names.get(key)
            aliases = tuple(vocabulary.aliases.get(key, ())) if vocabulary is not None else ()
            # A registry-proven ID may also have a more specific official locale vocabulary name.
            alternate = vocabulary.names.get(key) if vocabulary is not None else None
            if type(alternate) is str and alternate != label and alternate not in aliases:
                aliases += (alternate,)
            entry = MappingProxyType({"kind": kind, "id": identifier, "label": label,
                "translation_key": row.get("translation_key"), "tokens": MappingProxyType(dict(tokens)),
                "capabilities": frozenset(capabilities), "aliases": aliases})
            entries[key] = entry
            for command, token in tokens.items():
                if command in entry["capabilities"]:
                    token_index.setdefault((kind, command, token), set()).add(key)
            names = [identifier, *aliases]
            if identifier.startswith("minecraft:"):
                names.append(identifier.split(":", 1)[1])
            if type(label) is str:
                names.append(label)
            for name in names:
                index.setdefault((kind, self.normalize(name)), set()).add(key)
        self.entries = MappingProxyType(entries)
        self.index = MappingProxyType({key: tuple(sorted(values)) for key, values in index.items()})
        self.token_index = MappingProxyType({key: tuple(sorted(values)) for key, values in token_index.items()})

    @staticmethod
    def normalize(value):
        return FindTargetResolver.normalize(value)

    def matching(self, kind, phrase):
        return tuple(self.entries[key] for key in self.index.get((kind, self.normalize(phrase)), ()))

    def token_matches(self, kind, command, token):
        return tuple(self.entries[key] for key in self.token_index.get((kind, command, token), ()))

    def coverage(self):
        result = {}
        for kind in ("item", "block", "entity"):
            rows = [row for (entry_kind, _), row in self.entries.items() if entry_kind == kind]
            translated = sum(bool(type(row["label"]) is str and re.search(r"[가-힣]", row["label"])) for row in rows)
            ambiguous = sum(1 for row in rows if row["label"] and len(self.matching(kind, row["label"])) > 1)
            result[kind] = {"registered": len(rows), "korean_names": translated,
                            "missing_korean_names": len(rows) - translated, "ambiguous_targets": ambiguous}
        return result
