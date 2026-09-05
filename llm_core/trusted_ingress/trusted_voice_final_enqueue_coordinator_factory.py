#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.delivery import TrustedVoiceInputFinalEnqueueCoordinator


class TrustedVoiceFinalEnqueueCoordinatorFactory:
    def __init__(
        self,
        *,
        producer_registrar_factory,
        claim_aware_queue_sink_callback,
    ) -> None:
        if not callable(
            getattr(producer_registrar_factory, "begin_invocation", None)
        ):
            raise TypeError(
                "producer_registrar_factory.begin_invocation must be callable"
            )
        if not callable(claim_aware_queue_sink_callback):
            raise TypeError("claim_aware_queue_sink_callback must be callable")
        self._producer_registrar_factory = producer_registrar_factory
        self._claim_aware_queue_sink_callback = claim_aware_queue_sink_callback

    def create(
        self,
        input_event_adapter,
        *,
        pre_accept_observers=(),
        post_accept_observers=(),
    ):
        return TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=self._producer_registrar_factory,
            claim_aware_queue_sinks=(self._claim_aware_queue_sink_callback(),),
            pre_accept_observers=pre_accept_observers,
            post_accept_observers=post_accept_observers,
        )


__all__ = ("TrustedVoiceFinalEnqueueCoordinatorFactory",)
