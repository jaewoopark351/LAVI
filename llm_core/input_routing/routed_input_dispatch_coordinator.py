#20260905_kpopmodder: Sequence focused routed-input dispatch collaborators.
from __future__ import annotations

from core.logger import log_print

from .routed_input_dispatch_component_graph import (
    RoutedInputDispatchComponentGraph,
)
from .routed_input_dispatch_outcome import RoutedInputDispatchOutcome


class RoutedInputDispatchCoordinator:
    def __init__(
        self,
        *,
        response_publisher_callback,
        router=None,
        log_callback=log_print,
    ) -> None:
        self._components = RoutedInputDispatchComponentGraph(
            response_publisher_callback=response_publisher_callback,
            router=router,
            log_callback=log_callback,
        )

    def set_router(self, router) -> None:
        self._components.router_invocation.set_router(router)

    def dispatch(
        self,
        message,
        *,
        trusted_ingress_evidence=None,
    ) -> RoutedInputDispatchOutcome:
        components = self._components
        decision, terminal_outcome = components.router_invocation.invoke(
            message,
            trusted_ingress_evidence=trusted_ingress_evidence,
        )
        if terminal_outcome is not None:
            return terminal_outcome

        resolution = components.decision_resolver.resolve(decision)
        if resolution.terminal_outcome is not None:
            return resolution.terminal_outcome
        emission = components.external_response_publisher.publish(
            message=message,
            decision=decision,
            response_text=resolution.response_text,
        )
        if emission is None:
            return components.outcome_factory.suppressed()
        return components.outcome_factory.handled(emission)

    def prepare_response_for_yield(self, message, response: object) -> object:
        return self._components.chat_ui_yield_adapter.adapt(
            message,
            response,
        )


__all__ = ("RoutedInputDispatchCoordinator",)
