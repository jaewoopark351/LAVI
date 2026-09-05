#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .routed_response_delivery_observer import RoutedResponseDeliveryObserver
from .routed_response_request import RoutedResponseRequest


class RoutedResponseFullOutputSinkDelivery:
    def __init__(self, *, send_full_output_callback, observer) -> None:
        if not callable(send_full_output_callback):
            raise TypeError("send_full_output_callback must be callable")
        if type(observer) is not RoutedResponseDeliveryObserver:
            raise TypeError("observer must be an exact RoutedResponseDeliveryObserver")
        self._send_full_output_callback = send_full_output_callback
        self._observer = observer

    def deliver(
        self,
        request: RoutedResponseRequest,
        response_generation: object,
    ) -> bool:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        if not request.send_full_output:
            return False
        try:
            self._send_full_output_callback(request.text)
        except Exception:
            self._observer.observe_internal_failure("full_output")
            self._observer.observe_sink(
                request,
                sink="full_output_listener",
                response_generation=response_generation,
                delivered=False,
                reason="delivery_failed",
            )
            return False
        self._observer.observe_sink(
            request,
            sink="full_output_listener",
            response_generation=response_generation,
            delivered=True,
            reason="delivered",
        )
        return True


__all__ = ("RoutedResponseFullOutputSinkDelivery",)
