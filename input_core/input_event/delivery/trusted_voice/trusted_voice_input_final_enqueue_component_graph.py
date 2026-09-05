#20260905_kpopmodder: Assemble focused VoiceInput-final delivery collaborators.
from __future__ import annotations

from .binding import TrustedVoiceInputBindingFactory
from .diagnostics import TrustedVoiceDeliveryDiagnosticLogger
from .runtime import (
    TrustedVoiceDeliveryAbandoner,
    TrustedVoiceInputFinalEnqueueRuntime,
    TrustedVoicePostAcceptObserverNotifier,
    TrustedVoicePreAcceptObserverNotifier,
    TrustedVoiceQueueAcceptance,
    TrustedVoiceRegisteredDeliveryFactory,
)


class TrustedVoiceInputFinalEnqueueComponentGraph:
    def __init__(
        self,
        *,
        input_event_adapter,
        producer_registrar_factory,
        claim_aware_queue_sinks,
        pre_accept_observers,
        post_accept_observers,
        log_callback,
    ):
        self.binding = TrustedVoiceInputBindingFactory().create(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
        )
        self.diagnostics = TrustedVoiceDeliveryDiagnosticLogger(log_callback)
        self.delivery_factory = TrustedVoiceRegisteredDeliveryFactory(
            self.binding.invocation_owner
        )
        self.queue_acceptance = TrustedVoiceQueueAcceptance(
            claim_aware_queue_sinks,
            self.diagnostics,
        )
        self.pre_accept_notifier = TrustedVoicePreAcceptObserverNotifier(
            pre_accept_observers,
            self.diagnostics,
        )
        self.post_accept_notifier = TrustedVoicePostAcceptObserverNotifier(
            post_accept_observers,
            self.diagnostics,
        )
        self.abandoner = TrustedVoiceDeliveryAbandoner(self.diagnostics)
        self.runtime = TrustedVoiceInputFinalEnqueueRuntime(
            delivery_factory=self.delivery_factory,
            queue_acceptance=self.queue_acceptance,
            pre_accept_notifier=self.pre_accept_notifier,
            post_accept_notifier=self.post_accept_notifier,
            abandoner=self.abandoner,
        )


__all__ = ("TrustedVoiceInputFinalEnqueueComponentGraph",)
