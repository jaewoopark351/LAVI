#20260905_kpopmodder: Owns idempotent Input provider/listener/base shutdown ordering.
from __future__ import annotations


class InputComponentLifecycleCoordinator:
    def __init__(
        self,
        *,
        shutdown_state_callback,
        mark_shutdown_callback,
        shutdown_provider_bindings_callback,
        clear_provider_binding_requests_callback,
        clear_output_listeners_callback,
        base_shutdown_callback,
    ) -> None:
        callbacks = (
            shutdown_state_callback,
            mark_shutdown_callback,
            shutdown_provider_bindings_callback,
            clear_provider_binding_requests_callback,
            clear_output_listeners_callback,
            base_shutdown_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("Input lifecycle callbacks must be callable")
        self._shutdown_state_callback = shutdown_state_callback
        self._mark_shutdown_callback = mark_shutdown_callback
        self._shutdown_provider_bindings_callback = (
            shutdown_provider_bindings_callback
        )
        self._clear_provider_binding_requests_callback = (
            clear_provider_binding_requests_callback
        )
        self._clear_output_listeners_callback = clear_output_listeners_callback
        self._base_shutdown_callback = base_shutdown_callback

    def shutdown(self) -> None:
        if self._shutdown_state_callback():
            return
        self._mark_shutdown_callback()
        self._shutdown_provider_bindings_callback()
        self._clear_provider_binding_requests_callback()
        self._clear_output_listeners_callback()
        self._base_shutdown_callback()


__all__ = ("InputComponentLifecycleCoordinator",)
