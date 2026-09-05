#20260905_kpopmodder: Owns speech-style normalization and current-mode transitions.
from __future__ import annotations


class LlmSpeechStyleStateController:
    def __init__(
        self,
        *,
        helper_callback,
        current_mode_callback,
        update_mode_callback,
        default_mode: str,
    ) -> None:
        if not callable(helper_callback):
            raise TypeError("helper_callback must be callable")
        if not callable(current_mode_callback):
            raise TypeError("current_mode_callback must be callable")
        if not callable(update_mode_callback):
            raise TypeError("update_mode_callback must be callable")
        if type(default_mode) is not str or not default_mode:
            raise ValueError("default_mode must be a non-empty exact str")
        self._helper_callback = helper_callback
        self._current_mode_callback = current_mode_callback
        self._update_mode_callback = update_mode_callback
        self._default_mode = default_mode

    def normalize(self, value) -> str:
        return self._helper_callback().normalize(value)

    def initialize(self, value) -> str:
        return self.set_mode(value)

    def set_mode(self, value) -> str:
        mode = self.normalize(value)
        self._update_mode_callback(mode)
        return mode

    def current_mode(self) -> str:
        return self.normalize(self._current_mode_callback())

    def current_label(self) -> str:
        return self._helper_callback().label_for(self.current_mode())


__all__ = ("LlmSpeechStyleStateController",)
