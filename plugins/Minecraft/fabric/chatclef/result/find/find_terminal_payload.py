#20260914_kpopmodder: Decode exact, bounded FIND observations; do not infer success from free text.
from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Mapping
from uuid import UUID
import unicodedata

from ...intent.navigation.find import FindRequest


@dataclass(frozen=True, slots=True)
class FindTerminalPayload:
    request: FindRequest
    operation_id: str
    code: str
    kind: str
    registry_id: str
    label: str
    dimension: str
    position: tuple[int, ...]
    radius: int
    scanned: int
    scan_complete: bool
    observed: bool
    arrived: bool
    entity_uuid: str
    language_warnings: int
    suggestions: tuple[str, ...]
    scope: str

    SUCCESS = frozenset({"FOUND", "ARRIVED"})
    FAILURE = frozenset({"NOT_FOUND", "UNKNOWN_TARGET", "AMBIGUOUS_TARGET", "TARGET_LOST",
                         "NO_APPROACH", "APPROACH_TIMEOUT", "SEARCH_LIMIT", "SCOPE_CHANGED",
                         "PLAYER_UNAVAILABLE", "INTERNAL_ERROR"})
    _KEYS = frozenset({"schema_version", "operation_id", "query", "requested_kind", "mode", "code",
                       "kind", "registry_id", "label", "dimension", "position", "radius", "scanned",
                       "scan_complete", "observed", "arrived", "entity_uuid", "language_warnings",
                       "suggestions", "scope"})
    _ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)

    @classmethod
    def from_data(cls, data: object) -> FindTerminalPayload | None:
        value = data.get("find") if isinstance(data, Mapping) else None
        if not isinstance(value, Mapping) or set(value) != cls._KEYS:
            return None
        try:
            if type(value["schema_version"]) is not int or value["schema_version"] != 1:
                return None
            request = FindRequest(value["requested_kind"], value["query"], value["mode"])
            for field in ("operation_id", "code", "kind", "registry_id", "label", "dimension", "entity_uuid", "scope"):
                text = value[field]
                if type(text) is not str or len(text) > 256 or any(unicodedata.category(c)[0] == "C" for c in text):
                    return None
            if str(UUID(value["operation_id"])) != value["operation_id"]:
                return None
            code, kind = value["code"], value["kind"]
            if code not in cls.SUCCESS | cls.FAILURE or kind not in FindRequest.KINDS:
                return None
            if request.kind != "auto" and kind != request.kind:
                return None
            for field in ("scan_complete", "observed", "arrived"):
                if type(value[field]) is not bool:
                    return None
            for field in ("radius", "scanned", "language_warnings"):
                if type(value[field]) is not int or not 0 <= value[field] <= 2_147_483_647:
                    return None
            if value["radius"] != (32 if kind == "block" else 64):
                return None
            if value["scope"] != ("loaded_block_cube" if kind == "block" else "loaded_entity_sphere"):
                return None
            if value["scanned"] > (65 ** 3 if kind == "block" else 4096):
                return None
            position = value["position"]
            if type(position) not in (list, tuple) or len(position) not in (0, 3):
                return None
            if any(type(n) is not int or not -2_147_483_648 <= n <= 2_147_483_647 for n in position):
                return None
            suggestions = value["suggestions"]
            if type(suggestions) not in (list, tuple) or len(suggestions) > 5:
                return None
            for item in suggestions:
                if type(item) is not str or len(item) > 180:
                    return None
                candidate = FindRequest.parse("find " + item + " report")
                if candidate.kind not in {"entity", "block", "item"} or not cls._ID.fullmatch(candidate.query):
                    return None
            if bool(suggestions) != (code == "AMBIGUOUS_TARGET"):
                return None
            if code == "AMBIGUOUS_TARGET" and len(suggestions) < 2:
                return None
            success = code in cls.SUCCESS
            if value["observed"] is not success or value["arrived"] is not (code == "ARRIVED"):
                return None
            if success:
                if (not value["scan_complete"] or len(position) != 3 or kind == "auto"
                        or not value["label"] or not cls._ID.fullmatch(value["dimension"])
                        or (code == "FOUND") != (request.mode == "report")):
                    return None
            if value["registry_id"]:
                if kind == "player":
                    if not re.fullmatch(r"[A-Za-z0-9_]{1,16}", value["registry_id"]):
                        return None
                elif not cls._ID.fullmatch(value["registry_id"]):
                    return None
            elif success or code in {"NOT_FOUND", "TARGET_LOST", "NO_APPROACH", "APPROACH_TIMEOUT"}:
                return None
            if value["entity_uuid"] and str(UUID(value["entity_uuid"])) != value["entity_uuid"]:
                return None
            if kind == "block" and value["entity_uuid"]:
                return None
            if success and kind != "block" and not value["entity_uuid"]:
                return None
            if code == "NOT_FOUND" and (not value["scan_complete"] or position or kind == "auto"):
                return None
            if code in {"UNKNOWN_TARGET", "AMBIGUOUS_TARGET"} and (
                value["scan_complete"] or position or value["scanned"] != 0 or value["registry_id"]
            ):
                return None
            if code in {"NO_APPROACH", "APPROACH_TIMEOUT"} and (request.mode != "approach" or not value["scan_complete"]):
                return None
            return cls(request, value["operation_id"], code, kind, value["registry_id"], value["label"],
                       value["dimension"], tuple(position), value["radius"], value["scanned"],
                       value["scan_complete"], value["observed"], value["arrived"], value["entity_uuid"],
                       value["language_warnings"], tuple(suggestions), value["scope"])
        except (KeyError, TypeError, ValueError, AttributeError):
            return None
