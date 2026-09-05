#20260905_kpopmodder: Preserve the trusted local-Chat dispatch facade.
from __future__ import annotations

from .trusted_local import (
    TrustedLocalChatInputDispatchComponentGraph,
)


class TrustedLocalChatInputDispatchCoordinator:
    def __init__(
        self,
        *,
        input_event_adapter,
        producer_registrar_factory,
        registered_dispatch_callback,
        fallback_predict_callback,
    ):
        self._components = TrustedLocalChatInputDispatchComponentGraph(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
            registered_dispatch_callback=registered_dispatch_callback,
            fallback_predict_callback=fallback_predict_callback,
        )

    def dispatch(self, message, history, system_prompt):
        yield from self._components.runtime.dispatch(
            message,
            history,
            system_prompt,
        )


__all__ = ("TrustedLocalChatInputDispatchCoordinator",)
