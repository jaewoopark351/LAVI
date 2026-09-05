#20260905_kpopmodder: Coordinates validated and authorized routed-response publication.
from __future__ import annotations

from .routed_response_capability_authorizer import (
    RoutedResponseCapabilityAuthorizer,
)
from .routed_response_delivery_observer import RoutedResponseDeliveryObserver
from .routed_response_emission import RoutedResponseEmission
from .routed_response_request_validator import RoutedResponseRequestValidator
from .routed_response_sink_delivery import RoutedResponseSinkDelivery


class RoutedResponsePublicationCoordinator:
    def __init__(
        self,
        *,
        request_validator,
        capability_authorizer,
        sink_delivery,
        delivery_observer,
    ) -> None:
        if type(request_validator) is not RoutedResponseRequestValidator:
            raise TypeError("request_validator must be exact")
        if type(capability_authorizer) is not RoutedResponseCapabilityAuthorizer:
            raise TypeError("capability_authorizer must be exact")
        if type(sink_delivery) is not RoutedResponseSinkDelivery:
            raise TypeError("sink_delivery must be exact")
        if type(delivery_observer) is not RoutedResponseDeliveryObserver:
            raise TypeError("delivery_observer must be exact")
        self._request_validator = request_validator
        self._capability_authorizer = capability_authorizer
        self._sink_delivery = sink_delivery
        self._delivery_observer = delivery_observer

    def emit_external_response(
        self,
        text,
        *,
        source,
        send_output,
        send_full_output,
        remember_history,
        event_id,
        route_kind,
        response_kind,
    ) -> RoutedResponseEmission:
        request = self._request_validator.validate(
            text,
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
        )
        return self._sink_delivery.deliver(request)

    def emit_capability_response(
        self,
        text,
        *,
        emission_capability,
        event,
        source,
        response_kind,
        route_kind,
        send_output,
        send_full_output,
        remember_history,
    ) -> RoutedResponseEmission | None:
        request = self._request_validator.validate(
            text,
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            event_id=getattr(event, "event_id", None),
            route_kind=route_kind,
            response_kind=response_kind,
        )
        authorized, rejection_reason = self._capability_authorizer.authorize(
            request,
            emission_capability=emission_capability,
            event=event,
        )
        if authorized:
            return self._sink_delivery.deliver(request)
        if rejection_reason == "authorization_failed":
            self._delivery_observer.observe_internal_failure("authorization")
        self._delivery_observer.observe_requested_sinks(
            request,
            response_generation=None,
            delivered=False,
            reason=rejection_reason or "authorization_rejected",
        )
        return None

    def log_chat_ui_delivery(
        self,
        emission: object,
        *,
        delivered: bool,
        reason: str,
    ) -> bool:
        return self._delivery_observer.observe_chat_ui(
            emission,
            delivered=delivered,
            reason=reason,
        )


__all__ = ("RoutedResponsePublicationCoordinator",)
