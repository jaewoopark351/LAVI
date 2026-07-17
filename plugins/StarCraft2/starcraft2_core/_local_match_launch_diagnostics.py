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

class _LocalMatchLaunchDiagnostics:
    #20260713_kpopmodder: Track startup stage markers from LadDee/binary logs for startup
    # failure triage.
    def __init__(self):
        self.started_at = 0.0
        self.stages: Dict[str, float] = {}
        self.last_lines = deque(maxlen=20)

    def start(self) -> None:
        self.started_at = time.time()
        self.stages = {"start_called": self.started_at}

    def add_line(self, stream_name: str, line: str) -> None:
        text = str(line or "").strip()
        if not text:
            return
        marker = self._map_marker(text.lower())
        if marker:
            self.stages.setdefault(marker, time.time())
        self.last_lines.append(text)

    def finalize(self, exit_code: Any = None) -> Dict[str, Any]:
        elapsed = self._elapsed()
        exit_code_value = self._coerce_exit_code(exit_code)
        return {
            "started_at": self.started_at,
            "elapsed_sec": round(elapsed, 3),
            "exit_code": exit_code_value,
            "stage": self._derive_stage(),
            "stage_timestamps": dict(self.stages),
            "last_lines": list(self.last_lines),
            "launch_result": self._infer_launch_result(exit_code_value),
        }

    def snapshot(self) -> Dict[str, Any]:
        return {
            "started_at": self.started_at,
            "elapsed_sec": round(self._elapsed(), 3),
            "stage": self._derive_stage(),
            "stage_timestamps": dict(self.stages),
            "last_lines": list(self.last_lines),
            "launch_result": None,
        }

    def _derive_stage(self) -> str:
        if "finished_with_result" in self.stages:
            return "finished"
        if "starting_match" in self.stages:
            return "starting_match"
        if "creating_game" in self.stages:
            return "creating_game"
        if "starting_the_match" in self.stages:
            return "starting_the_match"
        if "starting_clients" in self.stages:
            return "starting_clients"
        return "running" if self.started_at else "not_started"

    def _infer_launch_result(self, exit_code: Optional[int]) -> Optional[str]:
        if exit_code is None:
            return None
        if exit_code == 0:
            return "success"
        if exit_code == 4:
            stage = self._derive_stage()
            return (
                "initialization_error"
                if stage not in {"starting_match", "creating_game", "finished"}
                else "match_runtime_error"
            )
        return "process_exit_failure"

    def _coerce_exit_code(self, value: Any) -> Optional[int]:
        if value is None:
            return None
        try:
            return int(value)
        except (TypeError, ValueError):
            fallback = getattr(value, "returncode", None)
            try:
                return None if fallback is None else int(fallback)
            except (TypeError, ValueError):
                return None

    def _map_marker(self, line_lower: str) -> Optional[str]:
        markers = {
            "starting the starcraft ii clients": "starting_clients",
            "creating the game": "creating_game",
            "starting the bots": "starting_the_match",
            "starting the match": "starting_the_match",
            "client changed status from in_game to ended": "finished_with_result",
            "finished with result:": "finished_with_result",
            "initializationerror": "finished_with_result",
            "initialization error": "finished_with_result",
        }
        for marker, stage in markers.items():
            if marker in line_lower:
                return stage
        return None

    def _elapsed(self) -> float:
        if not self.started_at:
            return 0.0
        return max(0.0, time.time() - self.started_at)
