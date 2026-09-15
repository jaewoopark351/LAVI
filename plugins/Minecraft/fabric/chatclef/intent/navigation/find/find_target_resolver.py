#20260915_kpopmodder: Resolve Korean FIND vocabulary in Python; compile only exact English registry IDs for Java.
from __future__ import annotations

from dataclasses import dataclass
import re
from types import MappingProxyType
import unicodedata

from .find_name_repository import FindNameRepository
from .find_request import FindRequest


class FindTargetResolutionError(ValueError):
    def __init__(self, code: str, suggestions=()):
        super().__init__(code)
        self.code = code
        self.suggestions = tuple(suggestions)


@dataclass(frozen=True)
class FindResolvedTarget:
    request: FindRequest
    label: str
    source: str


class FindTargetResolver:
    _ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)
    _SHORT_ID = re.compile(r"[a-z0-9_./-]+\Z", re.ASCII)

    def __init__(self, repository: FindNameRepository | None = None):
        self.repository = repository or FindNameRepository()
        index: dict[str, set[tuple[str, str]]] = {}
        for key, label in self.repository.names.items():
            kind, identifier = key
            aliases = (label, identifier.split(":", 1)[1], *self.repository.aliases.get(key, ()))
            for alias in aliases:
                index.setdefault(self.normalize(alias), set()).add(key)
        self._index = MappingProxyType({k: tuple(sorted(v)) for k, v in index.items()})

    @staticmethod
    def normalize(value: str) -> str:
        return re.sub(r"[\s_]+", "", unicodedata.normalize("NFKC", value).casefold())

    def resolve(self, request: FindRequest) -> FindResolvedTarget:
        if request.kind == "player":
            return FindResolvedTarget(request, request.query, "player_name")
        # Explicit IDs are never typo-corrected, translated, stripped or whitelisted.
        if ":" in request.query:
            if not self._ID.fullmatch(request.query):
                raise FindTargetResolutionError("find_invalid_registry_id")
            return FindResolvedTarget(request, self.label_for(request.kind, request.query), "explicit_id")
        # Exact short IDs have vanilla namespace semantics, matching Java; a mod needs its namespace.
        if self._SHORT_ID.fullmatch(request.query):
            identifier = "minecraft:" + request.query
            result = FindRequest(request.kind, identifier, request.mode)
            return FindResolvedTarget(result, self.label_for(request.kind, identifier), "short_id")
        query = self.normalize(request.query)
        candidates = self._matching(query, request.kind)
        if not candidates and request.kind == "block" and query.endswith("블록"):
            candidates = self._matching(query[:-2], "block")
        if request.kind == "auto":
            blocks = {identifier for kind, identifier in candidates if kind == "block"}
            candidates = tuple((kind, identifier) for kind, identifier in candidates
                               if not (kind == "item" and identifier in blocks and identifier in self.repository.block_items))
        if not candidates:
            raise FindTargetResolutionError("find_unknown_name")
        if len(candidates) != 1:
            raise FindTargetResolutionError("find_ambiguous_name", (f"{kind} {identifier}" for kind, identifier in candidates[:5]))
        kind, identifier = candidates[0]
        resolved = FindRequest(kind, identifier, request.mode)
        source = "json_alias" if any(self.normalize(a) == query for a in self.repository.aliases.get((kind, identifier), ())) else "korean_name"
        return FindResolvedTarget(resolved, self.label_for(kind, identifier), source)

    def _matching(self, query, kind):
        return tuple(key for key in self._index.get(query, ()) if kind == "auto" or key[0] == kind)

    def label_for(self, kind: str, identifier: str) -> str:
        if kind == "player":
            return identifier
        canonical = identifier if ":" in identifier else "minecraft:" + identifier
        label = self.repository.names.get((kind, canonical))
        if label:
            return label
        labels = {v for (k, i), v in self.repository.names.items() if i == canonical and kind == "auto"}
        return next(iter(labels)) if len(labels) == 1 else identifier
