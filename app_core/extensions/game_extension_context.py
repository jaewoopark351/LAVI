#20260706_kpopmodder: Added a shared context container for GameExtension initialization.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict


@dataclass
class GameExtensionContext:
    """
    Shared runtime resources that can be passed to GameExtension implementations.
    """

    app_composer: Any = None
    input_component: Any = None
    llm: Any = None
    translate: Any = None
    tts: Any = None
    vtuber: Any = None
    screen_vision: Any = None
    song_player: Any = None
    memory_store: Any = None
    memory_context_builder: Any = None
    screen_question_router: Any = None
    global_state: Any = None
    runtime_contexts: Any = None
    event_bus: Any = None
    runtime_state: Dict[str, Any] = field(default_factory=dict)
    shared_resources: Dict[str, Any] = field(default_factory=dict)

    def set_shared(self, key: str, value: Any) -> None:
        self.shared_resources[key] = value

    def get_shared(self, key: str, default: Any = None) -> Any:
        return self.shared_resources.get(key, default)

    def get_runtime_context(self, name: str):
        registry = self.runtime_contexts
        getter = getattr(registry, "get", None)
        if callable(getter):
            return getter(name)
        key = str(name or "").strip().lower()
        return self.runtime_state.setdefault(key, {})

    def publish_event(self, event) -> bool:
        emitter = getattr(self.event_bus, "emit", None)
        if not callable(emitter):
            return False
        return bool(emitter(event))

