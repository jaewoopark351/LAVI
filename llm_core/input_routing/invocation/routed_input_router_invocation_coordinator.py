#20260905_kpopmodder: Own trusted fail-closed and legacy fail-open router invocation.
from __future__ import annotations


class RoutedInputRouterInvocationCoordinator:
    def __init__(self, *, diagnostics, outcome_factory, router=None):
        self._diagnostics = diagnostics
        self._outcome_factory = outcome_factory
        self._router = router

    def set_router(self, router) -> None:
        self._router = router

    def capture_router(self):
        return self._router

    def invoke(self, message, *, trusted_ingress_evidence=None):
        return self.invoke_captured(
            self._router,
            message,
            trusted_ingress_evidence=trusted_ingress_evidence,
        )

    def invoke_captured(
        self,
        router: object,
        message,
        *,
        trusted_ingress_evidence=None,
    ):
        route = getattr(router, "route", None)
        trusted_route = getattr(
            router,
            "route_trusted_user_input",
            None,
        )
        if not callable(route) and not callable(trusted_route):
            return None, self._outcome_factory.unhandled()

        trusted_invocation = (
            trusted_ingress_evidence is not None and callable(trusted_route)
        )
        try:
            if trusted_invocation:
                return (
                    trusted_route(message, trusted_ingress_evidence),
                    None,
                )
            if callable(route):
                return route(message), None
            return None, self._outcome_factory.unhandled()
        except Exception:
            self._diagnostics.log_failure("router")
            if trusted_invocation:
                return None, self._outcome_factory.suppressed()
            return None, self._outcome_factory.unhandled()


__all__ = ("RoutedInputRouterInvocationCoordinator",)
