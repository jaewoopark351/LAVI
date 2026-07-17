#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Split StarCraft2 reaction side effects out of the runtime coordinator.
from __future__ import annotations

import json
from typing import Any, Dict

from core.logger import log_print
from .starcraft2_contracts import StarCraft2Event


#20260713_kpopmodder: Keep raw-event persistence separate from reaction policy and TTS flow.

class StarCraft2ReactionMemoryRecorder:
    def __init__(self, memory_store=None):
        self.memory_store = memory_store

    def store_event(self, event: StarCraft2Event) -> None:
        normalized = StarCraft2Event.from_mapping(event)
        add_raw_event = (
            getattr(self.memory_store, "add_raw_event", None)
            if self.memory_store
            else None
        )
        if not callable(add_raw_event):
            return

        try:
            payload = normalized.to_dict()
            add_raw_event(
                "starcraft2_game_event",
                json.dumps(payload, ensure_ascii=False, default=str),
                source="starcraft2",
                metadata={"event_type": normalized.event_type},
            )
        except Exception as exc:
            log_print(f"[StarCraft2Reaction] raw event store failed: {exc}")
