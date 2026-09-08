#20260905_kpopmodder: Sequence focused routed-input dispatch collaborators.
from __future__ import annotations

import threading

from core.logger import log_print

from .routed_input_dispatch_component_graph import (
    RoutedInputDispatchComponentGraph,
)
from .routed_input_dispatch_outcome import RoutedInputDispatchOutcome
from .publication import RoutedInputPublicationCustodyFailure


class RoutedInputDispatchCoordinator:
    def __init__(
        self,
        *,
        response_publisher_callback,
        router=None,
        log_callback=log_print,
    ) -> None:
        self._router_binding_lock = threading.RLock()
        self._components = RoutedInputDispatchComponentGraph(
            response_publisher_callback=response_publisher_callback,
            router=router,
            log_callback=log_callback,
        )

    def set_router(self, router) -> None:
        with self._router_binding_lock:
            self._components.router_invocation.set_router(router)
            self._components.publication_custody_guard.bind_router(router)

    def dispatch(
        self,
        message,
        *,
        trusted_ingress_evidence=None,
    ) -> RoutedInputDispatchOutcome:
        components = self._components
        with self._router_binding_lock:
            router_binding = components.router_invocation.capture_router()
            custody_ports = (
                components.publication_custody_guard.capture_ports()
            )
        decision, terminal_outcome = (
            components.router_invocation.invoke_captured(
                router_binding,
                message,
                trusted_ingress_evidence=trusted_ingress_evidence,
            )
        )
        publication_custody = (
            components.publication_custody_guard.claim(
                decision,
                ports=custody_ports,
            )
            if terminal_outcome is None
            else None
        )
        if terminal_outcome is not None:
            return terminal_outcome

        resolution = components.publication_custody_guard.resolve(
            decision=decision,
            resolver=components.decision_resolver,
            stage="dispatcher_initial_decision_resolution",
            custody=publication_custody,
        )
        if type(resolution) is RoutedInputPublicationCustodyFailure:
            return components.outcome_factory.suppressed()
        if resolution.terminal_outcome is not None:
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return resolution.terminal_outcome
        if not components.publication_acknowledger.wait_for_turn(decision):
            return components.outcome_factory.suppressed()
        try:
            ready_decision = (
                components.publication_acknowledger.resolve_ready_decision(
                    decision,
                    custody_protected=publication_custody is not None,
                )
            )
        except Exception as error:
            if publication_custody is None:
                raise
            components.publication_custody_guard.fail(
                publication_custody,
                stage="dispatcher_ready_decision_resolution",
                exception_class=type(error).__name__,
            )
            return components.outcome_factory.suppressed()
        if ready_decision is None:
            if publication_custody is not None:
                components.publication_custody_guard.fail(
                    publication_custody,
                    stage="dispatcher_ready_decision_resolution",
                    exception_class="none",
                )
            else:
                components.publication_acknowledger.acknowledge(
                    decision,
                    published=False,
                )
            return components.outcome_factory.suppressed()
        custody_failure = components.publication_custody_guard.validate(
            publication_custody,
            ready_decision,
        )
        if type(custody_failure) is RoutedInputPublicationCustodyFailure:
            return components.outcome_factory.suppressed()
        decision = ready_decision
        resolution = components.publication_custody_guard.resolve(
            decision=decision,
            resolver=components.decision_resolver,
            stage="dispatcher_ready_decision_resolution",
            custody=publication_custody,
        )
        if type(resolution) is RoutedInputPublicationCustodyFailure:
            return components.outcome_factory.suppressed()
        if resolution.terminal_outcome is not None:
            components.publication_acknowledger.acknowledge(
                decision,
                published=False,
            )
            return resolution.terminal_outcome
        try:
            emission = components.external_response_publisher.publish(
                message=message,
                decision=decision,
                response_text=resolution.response_text,
                raise_failure=publication_custody is not None,
            )
        except Exception as error:
            if publication_custody is None:
                raise
            components.publication_custody_guard.fail(
                publication_custody,
                stage="dispatcher_external_response_publication",
                exception_class=type(error).__name__,
            )
            return components.outcome_factory.suppressed()
        if publication_custody is not None and emission is None:
            components.publication_custody_guard.fail(
                publication_custody,
                stage="dispatcher_external_response_publication",
                exception_class="none",
            )
            return components.outcome_factory.suppressed()
        try:
            acknowledgement_present = (
                publication_custody is not None
                or getattr(
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
        except Exception as error:
            if publication_custody is None:
                raise
            components.publication_custody_guard.fail(
                publication_custody,
                stage="dispatcher_publication_commit_inspection",
                exception_class=type(error).__name__,
            )
            return components.outcome_factory.suppressed()
        if not delivered:
            if publication_custody is not None:
                components.publication_custody_guard.fail(
                    publication_custody,
                    stage="dispatcher_external_response_publication",
                    exception_class="none",
                )
            else:
                components.publication_acknowledger.acknowledge(
                    decision,
                    published=False,
                )
            return components.outcome_factory.suppressed()
        acknowledged = components.publication_acknowledger.acknowledge(
            (
                publication_custody.decision
                if publication_custody is not None
                else decision
            ),
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
