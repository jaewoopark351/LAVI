#20260905_kpopmodder: Own trusted-ingress and routed-input collaborator binding.
from __future__ import annotations

from input_core.input_event.normalization import LaviInputEventNormalizer
from llm_core.input_routing import (
    LlmPredictionDispatchCoordinator,
    RoutedInputDispatchCoordinator,
)
from llm_core.trusted_ingress import LlmTrustedIngressGraph


class LlmTrustedIngressComponentGraph:
    def __init__(self, facade) -> None:
        self._facade = facade

    def install_trusted_inputs(self) -> None:
        facade = self._facade
        facade.input_router = None
        facade.input_event_normalizer = LaviInputEventNormalizer()
        facade.trusted_ingress_graph = LlmTrustedIngressGraph(
            predict_callback=facade.predict_wrapper,
            claim_aware_queue_sink_callback=(
                lambda: facade.claim_aware_input_queue_sink
            ),
        )
        self.sync_trusted_ingress_compatibility_fields(
            facade.trusted_ingress_graph,
            include_local_chat=True,
        )

    def install_routing(self) -> None:
        facade = self._facade
        facade.routed_input_dispatch_coordinator = (
            RoutedInputDispatchCoordinator(
                response_publisher_callback=(
                    facade._get_routed_external_response_publisher
                ),
                router=facade.input_router,
            )
        )

    def ensure_input_event_normalizer(self):
        facade = self._facade
        normalizer = getattr(facade, "input_event_normalizer", None)
        if normalizer is None:
            normalizer = LaviInputEventNormalizer()
            facade.input_event_normalizer = normalizer
        return normalizer

    def ensure_trusted_ingress_graph(self):
        facade = self._facade
        graph = getattr(facade, "trusted_ingress_graph", None)
        if graph is None:
            graph = LlmTrustedIngressGraph(
                predict_callback=facade.predict_wrapper,
                claim_aware_queue_sink_callback=(
                    lambda: facade.claim_aware_input_queue_sink
                ),
                registry=getattr(
                    facade,
                    "trusted_user_input_ingress_claim_registry",
                    None,
                ),
                producer_registrar_factory=getattr(
                    facade,
                    "trusted_ingress_producer_registrar_factory",
                    None,
                ),
                local_chat_input_adapter=getattr(
                    facade,
                    "local_chat_input_adapter",
                    None,
                ),
            )
            facade.trusted_ingress_graph = graph
            self.sync_trusted_ingress_compatibility_fields(graph)
        return graph

    def sync_trusted_ingress_compatibility_fields(
        self,
        graph,
        *,
        include_local_chat=False,
    ) -> None:
        facade = self._facade
        facade.trusted_user_input_ingress_claim_registry = graph.registry
        facade.trusted_ingress_producer_registrar_factory = (
            graph.producer_registrar_factory
        )
        if include_local_chat:
            facade.local_chat_input_adapter = graph.local_chat_input_adapter
            facade.trusted_local_chat_input_dispatch_coordinator = (
                graph.local_chat_dispatch_coordinator
            )
            facade.local_chat_prediction_entrypoint = (
                graph.local_chat_prediction_entrypoint
            )
            facade.local_chat_interface_factory = (
                graph.local_chat_interface_factory
            )

    def ensure_local_chat_input_adapter(self):
        facade = self._facade
        adapter = self.ensure_trusted_ingress_graph().local_chat_input_adapter
        facade.local_chat_input_adapter = adapter
        return adapter

    def ensure_trusted_local_chat_input_dispatch_coordinator(self):
        facade = self._facade
        coordinator = (
            self.ensure_trusted_ingress_graph().local_chat_dispatch_coordinator
        )
        facade.trusted_local_chat_input_dispatch_coordinator = coordinator
        return coordinator

    def ensure_local_chat_prediction_entrypoint(self):
        facade = self._facade
        graph = self.ensure_trusted_ingress_graph()
        entrypoint = graph.local_chat_prediction_entrypoint
        facade.local_chat_prediction_entrypoint = entrypoint
        facade.local_chat_input_adapter = graph.local_chat_input_adapter
        return entrypoint

    def ensure_local_chat_interface_factory(self):
        facade = self._facade
        factory = (
            self.ensure_trusted_ingress_graph().local_chat_interface_factory
        )
        facade.local_chat_interface_factory = factory
        return factory

    def set_input_router(self, router) -> None:
        self._facade.input_router = router
        self.ensure_routed_input_dispatch_coordinator().set_router(router)

    def ensure_routed_input_dispatch_coordinator(self):
        facade = self._facade
        coordinator = getattr(
            facade,
            "routed_input_dispatch_coordinator",
            None,
        )
        if coordinator is None:
            coordinator = RoutedInputDispatchCoordinator(
                response_publisher_callback=(
                    facade._get_routed_external_response_publisher
                ),
                router=getattr(facade, "input_router", None),
            )
            facade.routed_input_dispatch_coordinator = coordinator
        else:
            coordinator.set_router(getattr(facade, "input_router", None))
        return coordinator

    def ensure_prediction_dispatch_coordinator(self):
        facade = self._facade
        coordinator = getattr(facade, "prediction_dispatch_coordinator", None)
        if coordinator is None:
            coordinator = LlmPredictionDispatchCoordinator(
                input_event_normalizer_callback=(
                    self.ensure_input_event_normalizer
                ),
                routed_dispatch_coordinator_callback=(
                    self.ensure_routed_input_dispatch_coordinator
                ),
                response_pipeline_callback=lambda: facade.response_pipeline,
                effective_system_prompt_callback=(
                    facade.build_effective_system_prompt
                ),
            )
            facade.prediction_dispatch_coordinator = coordinator
        return coordinator


__all__ = ("LlmTrustedIngressComponentGraph",)
