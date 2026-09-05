#20260905_kpopmodder: Owns normalized Input event listener registration and delivery.
from __future__ import annotations

from core.logger import log_print
from input_core.input_event.normalization import LaviInputEventNormalizer


class InputEventOutputDispatcher:
    def __init__(self, *, normalizer=None, log_callback=log_print) -> None:
        if normalizer is None:
            normalizer = LaviInputEventNormalizer()
        if not callable(getattr(normalizer, "normalize", None)):
            raise TypeError("normalizer.normalize must be callable")
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._normalizer = normalizer
        self._log_callback = log_callback
        self.listeners = []

    @property
    def normalizer(self):
        return self._normalizer

    def send(self, output, *, excluded_listeners=()) -> None:
        event = self._normalizer.normalize(output)
        self._log_callback(event.text)
        excluded = tuple(excluded_listeners)
        for listener in list(self.listeners):
            if listener in excluded:
                continue
            listener(event)

    def add(self, function) -> None:
        if function in self.listeners:
            return
        self.listeners.append(function)

    def remove(self, function) -> bool:
        removed = False
        while function in self.listeners:
            self.listeners.remove(function)
            removed = True
        return removed

    def clear(self) -> None:
        self.listeners.clear()


__all__ = ("InputEventOutputDispatcher",)
