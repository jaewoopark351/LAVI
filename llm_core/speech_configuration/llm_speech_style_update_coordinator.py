#20260905_kpopmodder: Coordinates one speech-style state change with persistence.
from __future__ import annotations

from .llm_speech_style_persistence import LlmSpeechStylePersistence
from .llm_speech_style_state_controller import LlmSpeechStyleStateController


class LlmSpeechStyleUpdateCoordinator:
    def __init__(self, *, state_controller, persistence) -> None:
        if type(state_controller) is not LlmSpeechStyleStateController:
            raise TypeError("state_controller must be exact")
        if type(persistence) is not LlmSpeechStylePersistence:
            raise TypeError("persistence must be exact")
        self._state_controller = state_controller
        self._persistence = persistence

    def update(self, value) -> str:
        mode = self._state_controller.set_mode(value)
        self._persistence.save_mode(mode)
        return mode


__all__ = ("LlmSpeechStyleUpdateCoordinator",)
