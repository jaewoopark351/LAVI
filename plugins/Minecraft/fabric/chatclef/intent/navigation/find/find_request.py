#20260914_kpopmodder: Share strict FIND slots across translation, raw grammar, and feedback.
from __future__ import annotations

from dataclasses import dataclass
import re
import unicodedata
from typing import Mapping


@dataclass(frozen=True)
class FindRequest:
    kind: str
    query: str
    mode: str = "approach"

    KINDS = frozenset({"auto", "entity", "block", "item", "player"})
    MODES = frozenset({"approach", "report"})

    def __post_init__(self) -> None:
        if type(self.kind) is not str or self.kind not in self.KINDS:
            raise ValueError("find_invalid_kind")
        if type(self.mode) is not str or self.mode not in self.MODES:
            raise ValueError("find_invalid_mode")
        if type(self.query) is not str or not 1 <= len(self.query) <= 128:
            raise ValueError("find_invalid_query")
        # No quoting/escaping or DSL separators. Names that need punctuation can use registry IDs.
        if self.query != " ".join(self.query.split()) or any(
            not (unicodedata.category(c)[0] in "LNM" or c in " _:/.-")
            for c in self.query
        ):
            raise ValueError("find_unsafe_query")
        if self.kind == "player" and re.fullmatch(r"[A-Za-z0-9_]{1,16}", self.query) is None:
            raise ValueError("find_invalid_player")

    @classmethod
    def from_slots(cls, slots: object) -> FindRequest:
        if not isinstance(slots, Mapping) or set(slots) != {"kind", "query", "mode"}:
            raise ValueError("find_invalid_slots")
        return cls(slots["kind"], slots["query"], slots["mode"])

    @classmethod
    def parse(cls, command: str) -> FindRequest:
        if type(command) is not str or len(command) > 180:
            raise ValueError("find_invalid_command")
        # Reject controls before whitespace normalization. Only one leading native prefix is legal.
        if any(unicodedata.category(c)[0] == "C" or c in ";#\\\"'" for c in command):
            raise ValueError("find_unsafe_command")
        tokens = command.strip().split()
        if not tokens or tokens.pop(0).lower() not in {"find", "@find"}:
            raise ValueError("find_invalid_command")
        if not tokens or any("@" in token for token in tokens):
            raise ValueError("find_missing_query")
        kind = tokens.pop(0).lower() if tokens[0].lower() in cls.KINDS else "auto"
        mode = tokens.pop().lower() if tokens and tokens[-1].lower() in cls.MODES else "approach"
        return cls(kind, " ".join(tokens), mode)

    def compile(self) -> str:
        return f"find {self.kind} {self.query} {self.mode}"

    def slots(self) -> dict[str, str]:
        return {"kind": self.kind, "query": self.query, "mode": self.mode}
