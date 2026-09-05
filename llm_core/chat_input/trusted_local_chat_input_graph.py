#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.adapters import LocalChatInputEventAdapter

from .local_chat_interface_factory import LocalChatInterfaceFactory
from .local_chat_prediction_entrypoint import LocalChatPredictionEntrypoint
from .trusted_local_chat_input_dispatch_coordinator import (
    TrustedLocalChatInputDispatchCoordinator,
)


class TrustedLocalChatInputGraph:
    def __init__(
        self,
        *,
        producer_registrar_factory,
        registered_dispatch_callback,
        predict_callback,
        input_event_adapter=None,
    ) -> None:
        self.input_event_adapter = input_event_adapter or LocalChatInputEventAdapter()
        self.dispatch_coordinator = TrustedLocalChatInputDispatchCoordinator(
            input_event_adapter=self.input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
            registered_dispatch_callback=registered_dispatch_callback,
            fallback_predict_callback=predict_callback,
        )
        self.prediction_entrypoint = LocalChatPredictionEntrypoint(
            input_event_adapter=self.input_event_adapter,
            predict_callback=predict_callback,
            dispatch_coordinator=self.dispatch_coordinator,
        )
        self.interface_factory = LocalChatInterfaceFactory(
            prediction_entrypoint=self.prediction_entrypoint,
        )


__all__ = ("TrustedLocalChatInputGraph",)
