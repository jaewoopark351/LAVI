#20260905_kpopmodder: Compose and hold focused trusted-ingress collaborators.
from __future__ import annotations

from llm_core.chat_input.trusted_local_chat_input_graph import (
    TrustedLocalChatInputGraph,
)

from .trusted_ingress_claim_graph import TrustedIngressClaimGraph
from .trusted_ingress_lease_dispatch_coordinator import (
    TrustedIngressLeaseDispatchCoordinator,
)
from .trusted_voice_final_enqueue_coordinator_factory import (
    TrustedVoiceFinalEnqueueCoordinatorFactory,
)
from .runtime import (
    LlmTrustedIngressEvidenceValidator,
    LlmTrustedIngressLeaseFacade,
    LlmTrustedVoiceEnqueueFacade,
)


class LlmTrustedIngressGraph:
    def __init__(
        self,
        *,
        predict_callback,
        claim_aware_queue_sink_callback,
        registry=None,
        producer_registrar_factory=None,
        local_chat_input_adapter=None,
    ) -> None:
        self.claim_graph = TrustedIngressClaimGraph(
            registry=registry,
            producer_registrar_factory=producer_registrar_factory,
        )
        self.lease_dispatch_coordinator = TrustedIngressLeaseDispatchCoordinator(
            predict_callback=predict_callback,
        )
        self._predict_callback = predict_callback
        self._local_chat_input_adapter = local_chat_input_adapter
        self._local_chat_graph = None
        self.voice_enqueue_factory = TrustedVoiceFinalEnqueueCoordinatorFactory(
            producer_registrar_factory=(
                self.claim_graph.producer_registrar_factory
            ),
            claim_aware_queue_sink_callback=claim_aware_queue_sink_callback,
        )
        self._evidence_validator = LlmTrustedIngressEvidenceValidator(
            self.claim_graph
        )
        self._lease_facade = LlmTrustedIngressLeaseFacade(
            self.lease_dispatch_coordinator
        )
        self._voice_enqueue_facade = LlmTrustedVoiceEnqueueFacade(
            self.voice_enqueue_factory
        )

    @property
    def registry(self):
        return self.claim_graph.registry

    @property
    def producer_registrar_factory(self):
        return self.claim_graph.producer_registrar_factory

    @property
    def local_chat_graph(self):
        if self._local_chat_graph is None:
            self._local_chat_graph = TrustedLocalChatInputGraph(
                input_event_adapter=self._local_chat_input_adapter,
                producer_registrar_factory=(
                    self.claim_graph.producer_registrar_factory
                ),
                registered_dispatch_callback=(
                    self.lease_dispatch_coordinator.accept_registered
                ),
                predict_callback=self._predict_callback,
            )
        return self._local_chat_graph

    @property
    def local_chat_input_adapter(self):
        return self.local_chat_graph.input_event_adapter

    @property
    def local_chat_dispatch_coordinator(self):
        return self.local_chat_graph.dispatch_coordinator

    @property
    def local_chat_prediction_entrypoint(self):
        return self.local_chat_graph.prediction_entrypoint

    @property
    def local_chat_interface_factory(self):
        return self.local_chat_graph.interface_factory

    def validate_consumed_evidence(self, event, evidence):
        return self._evidence_validator.validate(event, evidence)

    def accept_registered(self, delivery, history, system_prompt):
        yield from self._lease_facade.accept_registered(
            delivery,
            history,
            system_prompt,
        )

    def accept_queued(self, queue_delivery, history, system_prompt):
        yield from self._lease_facade.accept_queued(
            queue_delivery,
            history,
            system_prompt,
        )

    def dispatch_lease(self, lease, history, system_prompt):
        yield from self._lease_facade.dispatch_lease(
            lease,
            history,
            system_prompt,
        )

    def create_voice_final_enqueue_coordinator(
        self,
        input_event_adapter,
        *,
        pre_accept_observers=(),
        post_accept_observers=(),
    ):
        return self._voice_enqueue_facade.create(
            input_event_adapter,
            pre_accept_observers=pre_accept_observers,
            post_accept_observers=post_accept_observers,
        )


__all__ = ("LlmTrustedIngressGraph",)
