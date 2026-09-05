#20260905_kpopmodder: Preserve the LLM runtime lifecycle compatibility facade.
from __future__ import annotations

from .runtime import LlmRuntimeLifecycleComponentGraph


class LlmRuntimeLifecycleCoordinator:
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
    ) -> None:
        self._components = LlmRuntimeLifecycleComponentGraph(
            clear_pending_inputs_callback=clear_pending_inputs_callback,
            request_interrupt_callback=request_interrupt_callback,
            clear_listeners_callback=clear_listeners_callback,
            interrupt_message_callback=interrupt_message_callback,
            input_thread_callback=input_thread_callback,
            shutdown_state_callback=shutdown_state_callback,
            mark_shutdown_callback=mark_shutdown_callback,
            interrupt_subscription_callback=interrupt_subscription_callback,
            clear_interrupt_subscription_callback=(
                clear_interrupt_subscription_callback
            ),
            base_shutdown_callback=base_shutdown_callback,
        )

    def handle_interrupt(self) -> None:
        self._components.interrupt_handler.handle()

    def shutdown(self) -> None:
        self._components.shutdown_handler.shutdown()


__all__ = ("LlmRuntimeLifecycleCoordinator",)
