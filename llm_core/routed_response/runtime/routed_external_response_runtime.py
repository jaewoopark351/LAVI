#20260905_kpopmodder: Own routed external and capability response emission runtime.
from __future__ import annotations

from ..routed_response_emission import RoutedResponseEmission


class RoutedExternalResponseRuntime:
    def __init__(self, publication_coordinator):
        self._publication_coordinator = publication_coordinator

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
        return self._publication_coordinator.emit_external_response(
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
        return self._publication_coordinator.emit_capability_response(
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


__all__ = ("RoutedExternalResponseRuntime",)
