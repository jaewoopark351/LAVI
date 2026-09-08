#20260905_kpopmodder: Assemble focused routed-input dispatch collaborators.
from __future__ import annotations

from .decision import RoutedInputDecisionResolver
from .diagnostics import RoutedInputDispatchDiagnosticLogger
from .invocation import RoutedInputRouterInvocationCoordinator
from .outcomes import RoutedInputDispatchOutcomeFactory
from .publication import (
    RoutedInputExternalResponsePublisher,
    RoutedInputPublicationAcknowledger,
)
from .yielding import RoutedInputChatUiYieldAdapter


class RoutedInputDispatchComponentGraph:
    def __init__(
        self,
        *,
        response_publisher_callback,
        router,
        log_callback,
    ):
        self.outcome_factory = RoutedInputDispatchOutcomeFactory()
        self.diagnostics = RoutedInputDispatchDiagnosticLogger(log_callback)
        self.router_invocation = RoutedInputRouterInvocationCoordinator(
            diagnostics=self.diagnostics,
            outcome_factory=self.outcome_factory,
            router=router,
        )
        self.decision_resolver = RoutedInputDecisionResolver(
            self.outcome_factory
        )
        self.external_response_publisher = (
            RoutedInputExternalResponsePublisher(
                response_publisher_callback=response_publisher_callback,
                diagnostics=self.diagnostics,
            )
        )
        self.publication_acknowledger = RoutedInputPublicationAcknowledger(
            self.diagnostics
        )
        self.chat_ui_yield_adapter = RoutedInputChatUiYieldAdapter(
            response_publisher_callback
        )


__all__ = ("RoutedInputDispatchComponentGraph",)
