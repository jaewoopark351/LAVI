#20260707_kpopmodder: Added small runtime state object shared by StarCraft2 engines and UI.
from __future__ import annotations

import time
from collections import deque
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Iterable, List, Optional


@dataclass
class StarCraft2RuntimeState:
    running: bool = False
    engine: str = "internal_lav_bot"
    started_at: Optional[float] = None
    stopped_at: Optional[float] = None
    last_error: Optional[str] = None
    last_event: Optional[Dict[str, Any]] = None
    last_event_at: Optional[float] = None
    map_name: str = ""
    race: str = "Terran"
    enemy_race: str = "Zerg"
    enemy_difficulty: str = "Easy"
    result: str = ""
    duration_sec: float = 0.0
    minerals: int = 0
    vespene: int = 0
    supply_used: int = 0
    supply_cap: int = 0
    workers: int = 0
    army_count: int = 0
    process_pid: Optional[int] = None
    stdout_tail: List[str] = field(default_factory=list)
    stderr_tail: List[str] = field(default_factory=list)

    def mark_started(self, engine: str, config: Dict[str, Any] | None = None) -> None:
        config = config or {}
        self.running = True
        self.engine = str(engine or self.engine)
        self.started_at = time.time()
        self.stopped_at = None
        self.last_error = None
        self.result = ""
        self.duration_sec = 0.0
        self.map_name = str(config.get("map_name") or self.map_name or "")
        self.race = str(config.get("race") or self.race or "Terran")
        self.enemy_race = str(config.get("enemy_race") or self.enemy_race or "Zerg")
        self.enemy_difficulty = str(
            config.get("enemy_difficulty") or self.enemy_difficulty or "Easy"
        )

    def mark_stopped(self, result: str = "") -> None:
        self.running = False
        self.stopped_at = time.time()
        self.duration_sec = self._elapsed_since_start()
        if result:
            self.result = str(result)
        self.process_pid = None

    def mark_error(self, error: Any) -> None:
        self.last_error = str(error)
        self.running = False
        self.stopped_at = time.time()
        self.duration_sec = self._elapsed_since_start()

    def update_stats(
        self,
        minerals: Any = None,
        vespene: Any = None,
        supply_used: Any = None,
        supply_cap: Any = None,
        workers: Any = None,
        army_count: Any = None,
    ) -> None:
        self.minerals = self._safe_int(minerals, self.minerals)
        self.vespene = self._safe_int(vespene, self.vespene)
        self.supply_used = self._safe_int(supply_used, self.supply_used)
        self.supply_cap = self._safe_int(supply_cap, self.supply_cap)
        self.workers = self._safe_int(workers, self.workers)
        self.army_count = self._safe_int(army_count, self.army_count)

    def update_process(
        self,
        process_pid: Any = None,
        stdout_tail: Iterable[str] | None = None,
        stderr_tail: Iterable[str] | None = None,
    ) -> None:
        self.process_pid = self._safe_int_or_none(process_pid)
        if stdout_tail is not None:
            self.stdout_tail = [str(line) for line in stdout_tail][-20:]
        if stderr_tail is not None:
            self.stderr_tail = [str(line) for line in stderr_tail][-20:]

    def update_event(self, event: Dict[str, Any]) -> None:
        self.last_event = dict(event or {})
        self.last_event_at = time.time()
        event_type = str(self.last_event.get("event_type") or "")
        details = self.last_event.get("details")
        if not isinstance(details, dict):
            details = {}
        self.update_stats(
            minerals=details.get("minerals", self.minerals),
            vespene=details.get("vespene", self.vespene),
            supply_used=details.get("supply_used", self.supply_used),
            supply_cap=details.get("supply_cap", self.supply_cap),
            workers=details.get("workers", self.workers),
            army_count=details.get("army_count", self.army_count),
        )
        if event_type == "error":
            self.last_error = str(
                self.last_event.get("error") or details.get("error") or ""
            )
        if event_type == "game_ended":
            self.result = str(
                self.last_event.get("result") or details.get("result") or self.result or ""
            )
            self.running = False
            self.stopped_at = time.time()
            self.duration_sec = self._safe_float(
                details.get("elapsed_sec"),
                self._elapsed_since_start(),
            )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    @staticmethod
    def tail_deque(maxlen: int = 20) -> deque:
        return deque(maxlen=max(1, int(maxlen)))

    @staticmethod
    def _safe_int(value: Any, default: int = 0) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return int(default)

    @staticmethod
    def _safe_int_or_none(value: Any) -> Optional[int]:
        if value is None or value == "":
            return None
        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _safe_float(value: Any, default: float = 0.0) -> float:
        try:
            return float(value)
        except (TypeError, ValueError):
            return float(default)

    def _elapsed_since_start(self) -> float:
        if not self.started_at:
            return 0.0
        end_time = self.stopped_at or time.time()
        try:
            return round(max(0.0, float(end_time) - float(self.started_at)), 3)
        except (TypeError, ValueError):
            return 0.0
