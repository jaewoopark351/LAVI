#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .routed_response_delivery_observer import RoutedResponseDeliveryObserver
from .routed_response_request import RoutedResponseRequest


class RoutedResponseHistorySinkDelivery:
    def __init__(self, *, remember_history_callback=None, observer) -> None:
        if remember_history_callback is not None and not callable(
            remember_history_callback
        ):
            raise TypeError("remember_history_callback must be callable")
        if type(observer) is not RoutedResponseDeliveryObserver:
            raise TypeError("observer must be an exact RoutedResponseDeliveryObserver")
        self._remember_history_callback = remember_history_callback
        self._observer = observer

    def deliver(
        self,
        request: RoutedResponseRequest,
        response_generation: object,
    ) -> bool:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        if not request.remember_history:
            return False
        try:
            self._remember_history_callback(request.text, request.source)
        except Exception:
            self._observer.observe_internal_failure("history")
            self._observer.observe_sink(
                request,
                sink="history",
                response_generation=response_generation,
                delivered=False,
                reason="delivery_failed",
            )
            return False
        self._observer.observe_sink(
            request,
            sink="history",
            response_generation=response_generation,
            delivered=True,
            reason="delivered",
        )
        return True


__all__ = ("RoutedResponseHistorySinkDelivery",)
