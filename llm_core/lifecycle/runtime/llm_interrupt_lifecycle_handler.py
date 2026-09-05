#20260905_kpopmodder: Own the explicit LLM interrupt sequence.
from __future__ import annotations


class LlmInterruptLifecycleHandler:
    def __init__(
        self,
        *,
        clear_pending_inputs_callback,
        request_interrupt_callback,
        interrupt_message_callback,
    ):
        for name, callback in (
            ("clear_pending_inputs_callback", clear_pending_inputs_callback),
            ("request_interrupt_callback", request_interrupt_callback),
            ("interrupt_message_callback", interrupt_message_callback),
        ):
            if not callable(callback):
                raise TypeError(f"{name} must be callable")
        self._clear_pending_inputs_callback = clear_pending_inputs_callback
        self._request_interrupt_callback = request_interrupt_callback
        self._interrupt_message_callback = interrupt_message_callback

    def handle(self) -> None:
        self._clear_pending_inputs_callback()
        self._request_interrupt_callback()
        self._interrupt_message_callback()


__all__ = ("LlmInterruptLifecycleHandler",)
