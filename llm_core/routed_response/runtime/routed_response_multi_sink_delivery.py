#20260905_kpopmodder: Execute one generation across three focused response sinks.
from __future__ import annotations

from ..routed_response_emission import RoutedResponseEmission
from ..routed_response_request import RoutedResponseRequest
from ..presentation import RoutedResponseNonPreemptingDeliveryPolicy


class RoutedResponseMultiSinkDelivery:
    def __init__(
        self,
        *,
        begin_generation_callback,
        observer,
        sink_components,
    ):
        if not callable(begin_generation_callback):
            raise TypeError("begin_generation_callback must be callable")
        self._begin_generation_callback = begin_generation_callback
        self._observer = observer
        self._sink_components = sink_components

    def deliver(self, request: RoutedResponseRequest) -> RoutedResponseEmission:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        response_generation = self._begin_generation(request)
        output_delivered = self._sink_components.output_delivery.deliver(
            request,
            response_generation,
        )
        full_output_delivered = (
            self._sink_components.full_output_delivery.deliver(
                request,
                response_generation,
            )
        )
        history_remembered = self._sink_components.history_delivery.deliver(
            request,
            response_generation,
        )
        presentation_receipt = self._sink_components.ui_delivery.deliver(
            request,
            response_generation,
        )
        return RoutedResponseEmission(
            text=request.text,
            source=request.source,
            response_generation=response_generation,
            output_delivered=output_delivered,
            full_output_delivered=full_output_delivered,
            history_remembered=history_remembered,
            event_id=(
                request.event_id
                if type(request.event_id) is str and request.event_id
                else "none"
            ),
            route_kind=request.route_kind,
            response_kind=request.response_kind,
            delivery_mode=request.delivery_mode,
            presentation_metadata=request.presentation_metadata,
            presentation_receipt=presentation_receipt,
        )

    def _begin_generation(self, request: RoutedResponseRequest):
        if not RoutedResponseNonPreemptingDeliveryPolicy.begins_global_generation(
            request.delivery_mode
        ):
            return None
        try:
            return self._begin_generation_callback()
        except Exception:
            self._observer.observe_internal_failure("generation")
            self._observer.observe_requested_sinks(
                request,
                response_generation=None,
                delivered=False,
                reason="generation_failed",
            )
            raise


__all__ = ("RoutedResponseMultiSinkDelivery",)
