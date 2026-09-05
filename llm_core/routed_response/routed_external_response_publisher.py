#20260905_kpopmodder: Preserve the routed external-response public facade.
from __future__ import annotations

from core.logger import log_print

from .composition.routed_external_response_component_graph import (
    RoutedExternalResponseComponentGraph,
)
from .routed_response_emission import RoutedResponseEmission


class RoutedExternalResponsePublisher:
    def __init__(
        self,
        *,
        begin_generation_callback,
        build_output_payload_callback,
        send_output_callback,
        send_full_output_callback,
        remember_history_callback=None,
        emission_capability_consumer=None,
        delivery_logger=None,
        log_callback=log_print,
    ):
        self._components = RoutedExternalResponseComponentGraph(
            begin_generation_callback=begin_generation_callback,
            build_output_payload_callback=build_output_payload_callback,
            send_output_callback=send_output_callback,
            send_full_output_callback=send_full_output_callback,
            remember_history_callback=remember_history_callback,
            emission_capability_consumer=emission_capability_consumer,
            delivery_logger=delivery_logger,
            log_callback=log_callback,
        )

    @property
    def _delivery_observer(self):
        return self._components.delivery_observer

    @property
    def _request_validator(self):
        return self._components.request_validator

    @property
    def _capability_authorizer(self):
        return self._components.capability_authorizer

    @property
    def _sink_delivery(self):
        return self._components.sink_delivery

    @property
    def _publication_coordinator(self):
        return self._components.publication_coordinator

    def emit_external_response(
        self,
        text,
        *,
        source="minecraft_chatclef",
        send_output=True,
        send_full_output=False,
        remember_history=False,
        event_id=None,
        route_kind="minecraft_chatclef_external",
        response_kind="external",
    ) -> RoutedResponseEmission:
        return self._components.runtime.emit_external_response(
            text,
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
        )

    def emit_capability_response(
        self,
        text,
        *,
        emission_capability,
        event,
        source="minecraft_chatclef",
        response_kind="immediate",
        route_kind="minecraft_command",
        send_output=True,
        send_full_output=False,
        remember_history=False,
    ) -> RoutedResponseEmission | None:
        return self._components.runtime.emit_capability_response(
            text,
            emission_capability=emission_capability,
            event=event,
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            route_kind=route_kind,
            response_kind=response_kind,
        )

    def log_chat_ui_delivery(
        self,
        emission: object,
        *,
        delivered: bool,
        reason: str,
    ) -> bool:
        return self._components.chat_ui_diagnostics.log(
            emission,
            delivered=delivered,
            reason=reason,
        )


__all__ = ("RoutedExternalResponsePublisher",)
