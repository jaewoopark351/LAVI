#20260905_kpopmodder: Assemble independent LLM interrupt and shutdown handlers.
from __future__ import annotations

from .llm_interrupt_lifecycle_handler import LlmInterruptLifecycleHandler
from .llm_shutdown_lifecycle_handler import LlmShutdownLifecycleHandler


class LlmRuntimeLifecycleComponentGraph:
    def __init__(
        self,
        *,
        clear_pending_inputs_callback,
        request_interrupt_callback,
        clear_listeners_callback,
        interrupt_message_callback,
        input_thread_callback,
        shutdown_state_callback,
        mark_shutdown_callback,
        interrupt_subscription_callback,
        clear_interrupt_subscription_callback,
        base_shutdown_callback,
    ):
        self.interrupt_handler = LlmInterruptLifecycleHandler(
            clear_pending_inputs_callback=clear_pending_inputs_callback,
            request_interrupt_callback=request_interrupt_callback,
            interrupt_message_callback=interrupt_message_callback,
        )
        self.shutdown_handler = LlmShutdownLifecycleHandler(
            clear_pending_inputs_callback=clear_pending_inputs_callback,
            request_interrupt_callback=request_interrupt_callback,
            clear_listeners_callback=clear_listeners_callback,
            input_thread_callback=input_thread_callback,
            shutdown_state_callback=shutdown_state_callback,
            mark_shutdown_callback=mark_shutdown_callback,
            interrupt_subscription_callback=interrupt_subscription_callback,
            clear_interrupt_subscription_callback=(
                clear_interrupt_subscription_callback
            ),
            base_shutdown_callback=base_shutdown_callback,
        )


__all__ = ("LlmRuntimeLifecycleComponentGraph",)
