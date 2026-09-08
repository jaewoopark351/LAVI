#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from core.logger import log_print

from .command_feedback_delivery_logger import CommandFeedbackDeliveryLogger
from .routed_response_emission import RoutedResponseEmission
from .routed_response_request import RoutedResponseRequest


class RoutedResponseDeliveryObserver:
    _INTERNAL_BOUNDARIES = frozenset(
        {"authorization", "generation", "output", "full_output", "history"}
    )

    def __init__(
        self,
        *,
        delivery_logger=None,
        log_callback=log_print,
    ) -> None:
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        if delivery_logger is not None and not callable(
            getattr(delivery_logger, "log", None)
        ):
            raise TypeError("delivery_logger must expose callable log")
        self._log_callback = log_callback
        self._delivery_logger = delivery_logger or CommandFeedbackDeliveryLogger(
            log_callback
        )

    def observe_requested_sinks(
        self,
        request: RoutedResponseRequest,
        *,
        response_generation: object,
        delivered: bool,
        reason: str,
    ) -> None:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        for requested, sink in (
            (request.send_output, "output_listener"),
            (request.send_full_output, "full_output_listener"),
            (request.remember_history, "history"),
        ):
            if requested:
                self.observe_sink(
                    request,
                    sink=sink,
                    response_generation=response_generation,
                    delivered=delivered,
                    reason=reason,
                )

    def observe_sink(
        self,
        request: RoutedResponseRequest,
        *,
        sink: str,
        response_generation: object,
        delivered: bool,
        reason: str,
    ) -> bool:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        return self._log_command_delivery(
            source=request.source,
            event_id=request.event_id,
            route_kind=request.route_kind,
            response_kind=request.response_kind,
            sink=sink,
            response_generation=response_generation,
            delivered=delivered,
            reason=reason,
            delivery_mode=request.delivery_mode,
        )

    def observe_chat_ui(
        self,
        emission: object,
        *,
        delivered: bool,
        reason: str,
    ) -> bool:
        if type(emission) is not RoutedResponseEmission:
            return False
        return self._log_command_delivery(
            source=emission.source,
            event_id=emission.event_id,
            route_kind=emission.route_kind,
            response_kind=emission.response_kind,
            sink="chat_ui",
            response_generation=emission.response_generation,
            delivered=delivered,
            reason=reason,
            delivery_mode=emission.delivery_mode,
        )

    def observe_delivery_fact(
        self,
        *,
        source: str,
        event_id: object,
        route_kind: str,
        response_kind: str,
        sink: str,
        response_generation: object,
        delivered: bool,
        reason: str,
        delivery_mode: str = "current_input",
    ) -> bool:
        return self._log_command_delivery(
            source=source,
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            sink=sink,
            response_generation=response_generation,
            delivered=delivered,
            reason=reason,
            delivery_mode=delivery_mode,
        )

    def observe_internal_failure(self, boundary: str) -> None:
        bounded_boundary = (
            boundary if boundary in self._INTERNAL_BOUNDARIES else "invalid"
        )
        try:
            self._log_callback(
                "[RoutedExternalResponsePublisher] delivery failed: "
                f"boundary={bounded_boundary}"
            )
        except Exception:
            return

    def _log_command_delivery(
        self,
        *,
        source: str,
        event_id: object,
        route_kind: str,
        response_kind: str,
        sink: str,
        response_generation: object,
        delivered: bool,
        reason: str,
        delivery_mode: str = "current_input",
    ) -> bool:
        if source != "minecraft_chatclef":
            return False
        try:
            return self._delivery_logger.log(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                sink=sink,
                response_generation=response_generation,
                delivered=delivered,
                reason=reason,
                delivery_mode=delivery_mode,
            )
        except Exception:
            return False


__all__ = ("RoutedResponseDeliveryObserver",)
