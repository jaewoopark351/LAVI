#20260905_kpopmodder: Assemble focused trusted local-Chat dispatch collaborators.
from __future__ import annotations

from .binding import TrustedLocalChatInputBindingFactory
from .runtime import (
    TrustedLocalChatDeliveryAbandoner,
    TrustedLocalChatFallbackDispatcher,
    TrustedLocalChatInputDispatchRuntime,
    TrustedLocalChatRegisteredDeliveryDispatcher,
    TrustedLocalChatRegisteredDeliveryFactory,
)


class TrustedLocalChatInputDispatchComponentGraph:
    def __init__(
        self,
        *,
        input_event_adapter,
        producer_registrar_factory,
        registered_dispatch_callback,
        fallback_predict_callback,
    ):
        self.binding = TrustedLocalChatInputBindingFactory().create(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
        )
        self.delivery_factory = TrustedLocalChatRegisteredDeliveryFactory(
            self.binding.invocation_owner
        )
        self.fallback_dispatcher = TrustedLocalChatFallbackDispatcher(
            fallback_predict_callback
        )
        self.abandoner = TrustedLocalChatDeliveryAbandoner()
        self.registered_dispatcher = (
            TrustedLocalChatRegisteredDeliveryDispatcher(
                dispatch_callback=registered_dispatch_callback,
                abandoner=self.abandoner,
            )
        )
        self.runtime = TrustedLocalChatInputDispatchRuntime(
            delivery_factory=self.delivery_factory,
            fallback_dispatcher=self.fallback_dispatcher,
            registered_dispatcher=self.registered_dispatcher,
        )


__all__ = ("TrustedLocalChatInputDispatchComponentGraph",)
