#20260715_kpopmodder: Bridge SC2-specific events into the shared GameEventBus without replacing the SC2 bus.
from __future__ import annotations

from typing import Any, Dict, Optional

from app_core.extensions import GameEventDTO
from core.logger import log_print

from .starcraft2_contracts import StarCraft2Event


class StarCraft2GameEventBridge:
    #20260715_kpopmodder: Mirror SC2 events to the app-wide observer bus while preserving legacy subscribers.
    def __init__(self, game_event_bus: Any = None, game_name: str = "starcraft2"):
        self.game_event_bus = game_event_bus
        self.game_name = str(game_name or "starcraft2")

    def set_event_bus(self, game_event_bus: Any = None) -> None:
        self.game_event_bus = game_event_bus

    def emit(self, event: Dict[str, Any] | StarCraft2Event | None) -> bool:
        emitter = getattr(self.game_event_bus, "emit", None)
        if not callable(emitter):
            return False
        payload = StarCraft2Event.from_mapping(event).to_dict()
        if not payload.get("event_type"):
            return False
        details = dict(payload.get("details") or {})
        details.setdefault("sc2_source", payload.get("source"))
        details.setdefault("engine", payload.get("engine"))
        try:
            return bool(
                emitter(
                    GameEventDTO(
                        event_type=str(payload.get("event_type") or ""),
                        game=self.game_name,
                        source="starcraft2",
                        details=details,
                        time=float(payload.get("time") or 0.0),
                    )
                )
            )
        except Exception as e:
            log_print(f"[StarCraft2GameEventBridge] common event emit failed: {e}")
            return False
