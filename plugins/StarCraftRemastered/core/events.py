# #20260701_kpopmodder: Added a small event bus mirroring BWAPI AIModule callback names.
# import time
# from dataclasses import dataclass, field
# from enum import Enum
# from typing import Any, Callable, Dict, List


# class StarCraftEventType(str, Enum):
#     ON_START = "onStart"
#     ON_FRAME = "onFrame"
#     ON_END = "onEnd"
#     ON_UNIT_CREATE = "onUnitCreate"
#     ON_UNIT_DISCOVER = "onUnitDiscover"
#     ON_UNIT_DESTROY = "onUnitDestroy"
#     ON_UNIT_MORPH = "onUnitMorph"
#     ON_UNIT_COMPLETE = "onUnitComplete"


# @dataclass
# class StarCraftEvent:
#     #20260701_kpopmodder: Carries callback payloads without requiring native BWAPI bindings.
#     event_type: StarCraftEventType
#     payload: Dict[str, Any] = field(default_factory=dict)
#     created_at: float = field(default_factory=time.time)


# class StarCraftEventBus:
#     #20260701_kpopmodder: Lets future SAIDA adapters subscribe to BWAPI-like runtime events.
#     def __init__(self):
#         self._listeners: Dict[StarCraftEventType, List[Callable]] = {}

#     def add_listener(self, event_type, callback):
#         event_type = StarCraftEventType(event_type)
#         listeners = self._listeners.setdefault(event_type, [])
#         if callback not in listeners:
#             listeners.append(callback)

#     def remove_listener(self, event_type, callback):
#         event_type = StarCraftEventType(event_type)
#         listeners = self._listeners.get(event_type, [])
#         while callback in listeners:
#             listeners.remove(callback)

#     def emit(self, event_type, **payload):
#         event = StarCraftEvent(StarCraftEventType(event_type), dict(payload))
#         for callback in tuple(self._listeners.get(event.event_type, [])):
#             callback(event)
#         return event
