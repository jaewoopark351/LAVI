#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Add deterministic local-match launch argument template builder and launch diagnostics.
from __future__ import annotations

from collections import deque
from typing import Any, Dict, List, Optional, Set

import time


_KNOWN_RACES = {"terran", "zerg", "protoss", "random"}
_KNOWN_BOOLEAN_ARGS = {"--realtime", "--no-realtime"}
_KNOWN_VALUE_ARGS = {
    "--map",
    "--race",
    "--bot-race",
    "--bot-dir",
    "--config",
    "--bot",
    "--human-name",
}


def _normalize_race(value: str, fallback: str = "Terran") -> str:
    text = str(value or "").strip().lower()
    if text in _KNOWN_RACES:
        return text.title() if text != "random" else "Random"
    fallback_text = str(fallback or "").strip().lower()
    return fallback_text.title() if fallback_text in _KNOWN_RACES else "Terran"

class _LocalMatchTemplateBuildResult:
    def __init__(
        self,
        args: List[str],
        dropped_args: List[str],
        warnings: List[str],
        normalized_args: Dict[str, Optional[str]],
    ):
        self.args = args
        self.dropped_args = dropped_args
        self.warnings = warnings
        self.normalized_args = normalized_args

    def as_dict(self) -> Dict[str, Any]:
        return {
            "args": list(self.args),
            "dropped_args": list(self.dropped_args),
            "warnings": list(self.warnings),
            "normalized_args": dict(self.normalized_args),
        }
