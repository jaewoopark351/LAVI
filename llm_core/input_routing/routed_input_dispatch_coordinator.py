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
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return resolution.terminal_outcome
        if not components.publication_acknowledger.wait_for_turn(decision):
            return components.outcome_factory.suppressed()
        ready_decision = (
            components.publication_acknowledger.resolve_ready_decision(
                decision
            )
        )
        if ready_decision is None:
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return components.outcome_factory.suppressed()
        decision = ready_decision
        resolution = components.decision_resolver.resolve(decision)
        if resolution.terminal_outcome is not None:
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return resolution.terminal_outcome
        emission = components.external_response_publisher.publish(
            message=message,
            decision=decision,
            response_text=resolution.response_text,
        )
        acknowledgement_present = (
            getattr(
                decision,
                "response_publication_acknowledgement",
                None,
            )
            is not None
        )
        delivered = bool(
            emission is not None
            and (
                not acknowledgement_present
                or getattr(emission, "output_delivered", False) is True
            )
        )
        if not delivered:
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return components.outcome_factory.suppressed()
        acknowledged = components.publication_acknowledger.acknowledge(
            decision,
            published=True,
        )
        if acknowledgement_present and not acknowledged:
            return components.outcome_factory.suppressed()
        return components.outcome_factory.handled(emission)

    def prepare_response_for_yield(self, message, response: object) -> object:
        return self._components.chat_ui_yield_adapter.adapt(
            message,
            response,
        )


__all__ = ("RoutedInputDispatchCoordinator",)
