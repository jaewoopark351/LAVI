#20260905_kpopmodder: Own idempotent LLM shutdown and thread joining.
from __future__ import annotations

import threading


class LlmShutdownLifecycleHandler:
    def __init__(
        self,
        *,
        clear_pending_inputs_callback,
        request_interrupt_callback,
        clear_listeners_callback,
        input_thread_callback,
        shutdown_state_callback,
        mark_shutdown_callback,
        interrupt_subscription_callback,
        clear_interrupt_subscription_callback,
        base_shutdown_callback,
    ):
        callbacks = (
            ("clear_pending_inputs_callback", clear_pending_inputs_callback),
            ("request_interrupt_callback", request_interrupt_callback),
            ("clear_listeners_callback", clear_listeners_callback),
            ("input_thread_callback", input_thread_callback),
            ("shutdown_state_callback", shutdown_state_callback),
            ("mark_shutdown_callback", mark_shutdown_callback),
            (
                "interrupt_subscription_callback",
                interrupt_subscription_callback,
            ),
            (
                "clear_interrupt_subscription_callback",
                clear_interrupt_subscription_callback,
            ),
            ("base_shutdown_callback", base_shutdown_callback),
        )
        for name, callback in callbacks:
            if not callable(callback):
                raise TypeError(f"{name} must be callable")
        self._clear_pending_inputs_callback = clear_pending_inputs_callback
        self._request_interrupt_callback = request_interrupt_callback
        self._clear_listeners_callback = clear_listeners_callback
        self._input_thread_callback = input_thread_callback
        self._shutdown_state_callback = shutdown_state_callback
        self._mark_shutdown_callback = mark_shutdown_callback
        self._interrupt_subscription_callback = interrupt_subscription_callback
        self._clear_interrupt_subscription_callback = (
            clear_interrupt_subscription_callback
        )
        self._base_shutdown_callback = base_shutdown_callback

    def shutdown(self) -> None:
        if self._shutdown_state_callback():
            return
        self._mark_shutdown_callback()
        subscription = self._interrupt_subscription_callback()
        if subscription is not None:
            subscription.unsubscribe()
            self._clear_interrupt_subscription_callback()
        self._clear_pending_inputs_callback()
        self._request_interrupt_callback()
        self._clear_listeners_callback()
        input_thread = self._input_thread_callback()
        if (
            input_thread is not None
            and input_thread.is_alive()
            and threading.current_thread() != input_thread
        ):
            input_thread.join(timeout=0.3)
        self._base_shutdown_callback()


__all__ = ("LlmShutdownLifecycleHandler",)
