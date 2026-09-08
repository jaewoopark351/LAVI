#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Deliver typed UI presentations independently from output dispatch.
#20260908_kpopmodder: Derive lifecycle presentation identity before one bounded UI enqueue.
from __future__ import annotations

from ...routed_response_request import RoutedResponseRequest
from ..routed_response_presentation_receipt import (
    RoutedResponsePresentationReceipt,
)
from .routed_response_ui_presentation_adapter import (
    RoutedResponseUiPresentationAdapter,
)
from .routed_response_ui_presentation_identity import (
    RoutedResponseUiPresentationIdentity,
)


class RoutedResponseUiSinkDelivery:
    def __init__(
        self,
        *,
        adapter,
        presentation_callback,
        observer,
    ) -> None:
        if type(adapter) is not RoutedResponseUiPresentationAdapter:
            raise TypeError("adapter must be exact")
        if presentation_callback is not None and not callable(
            presentation_callback
        ):
            raise TypeError("presentation_callback must be callable")
        self._adapter = adapter
        self._presentation_callback = presentation_callback
        self._observer = observer

    def deliver(
        self,
        request: RoutedResponseRequest,
        response_generation: object,
    ):
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        if not request.send_ui:
            return None
        normalized_event_id = (
            request.event_id
            if type(request.event_id) is str and request.event_id
            else "none"
        )
        accepted = False
        reason = "disabled"
        if self._presentation_callback is not None:
            try:
                presentation_identity = (
                    RoutedResponseUiPresentationIdentity.from_response(
                        event_id=normalized_event_id,
                        route_kind=request.route_kind,
                        response_kind=request.response_kind,
                        response_source=request.source,
                        source_kind=(
                            request.presentation_metadata.source_kind
                        ),
                        badge_label=(
                            request.presentation_metadata.badge_label
                        ),
                    )
                )
                message = self._adapter.render(
                    request.text,
                    request.presentation_metadata,
                    presentation_identity=presentation_identity,
                )
                accepted = self._presentation_callback(message) is True
                reason = "enqueued" if accepted else "delivery_failed"
            except Exception:
                reason = "delivery_failed"
        receipt = RoutedResponsePresentationReceipt(
            event_id=normalized_event_id,
            sink=RoutedResponseUiPresentationAdapter.SINK,
            accepted=accepted,
            reason=reason,
        )
        self._observer.observe_sink(
            request,
            sink=receipt.sink,
            response_generation=response_generation,
            delivered=receipt.accepted,
            reason=receipt.reason,
        )
        return receipt


__all__ = ("RoutedResponseUiSinkDelivery",)
