#20260905_kpopmodder: Owns the compatibility API for LLM output listener registration.
from __future__ import annotations

from llm_core.event_dispatcher import LLMEventDispatcher


class LlmOutputListenerRegistry:
    def __init__(self, dispatcher=None) -> None:
        if dispatcher is None:
            dispatcher = LLMEventDispatcher()
        self._validate_dispatcher(dispatcher)
        self.dispatcher = dispatcher

    @property
    def output_event_listeners(self):
        return self.dispatcher.output_event_listeners

    @output_event_listeners.setter
    def output_event_listeners(self, value) -> None:
        self.dispatcher.output_event_listeners = value

    @property
    def full_output_event_listeners(self):
        return self.dispatcher.full_output_event_listeners

    @full_output_event_listeners.setter
    def full_output_event_listeners(self, value) -> None:
        self.dispatcher.full_output_event_listeners = value

    def send_output(self, output) -> None:
        self.dispatcher.send_output(output)

    def send_full_output(self, output) -> None:
        self.dispatcher.send_full_output(output)

    def add(self, function, *, full_response: bool = False) -> None:
        self.dispatcher.add_output_event_listener(
            function,
            full_response=full_response,
        )

    def remove(self, function, *, full_response: bool = False) -> bool:
        return self.dispatcher.remove_output_event_listener(
            function,
            full_response=full_response,
        )

    def clear(self) -> None:
        self.dispatcher.clear_listeners()

    @staticmethod
    def _validate_dispatcher(dispatcher) -> None:
        required = (
            "send_output",
            "send_full_output",
            "add_output_event_listener",
            "remove_output_event_listener",
            "clear_listeners",
        )
        if not all(callable(getattr(dispatcher, name, None)) for name in required):
            raise TypeError("dispatcher does not satisfy the LLM output contract")


__all__ = ("LlmOutputListenerRegistry",)
