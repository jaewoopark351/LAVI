#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Extract ladder-proxy event parsing and engine event routing.

from __future__ import annotations

import json
import re
import time

from .starcraft2_contracts import StarCraft2Event
from .starcraft2_event_bus import StarCraft2EventBus
from core.logger import log_print

class StarCraft2EngineEventService:
    #20260715_kpopmodder: Public event-state adapter for SC2 engine events.
    def __init__(self, state, status_event_callback=None, event_bus: StarCraft2EventBus | None = None):
        self.state = state
        self.event_bus = event_bus or StarCraft2EventBus()

    def set_status_event_callback(self, callback):
        #20260714_kpopmodder: Keep API compatibility for legacy callers,
        # but route callback through subscribe path only. None clears it.
        if self.event_bus is not None:
            self.event_bus.set_status_event_callback(callback)

    def update_state(self, event: StarCraft2Event) -> None:
        normalized = StarCraft2Event.from_mapping(event)
        #20260715_kpopmodder: RuntimeState now receives the DTO directly.
        self.state.update_event(normalized)
        if self.event_bus is not None:
            self.event_bus.emit(normalized)
