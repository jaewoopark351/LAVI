#20260905_kpopmodder: Preserve the routed-response sink delivery facade.
from __future__ import annotations

from .composition.routed_response_sink_component_graph import (
    RoutedResponseSinkComponentGraph,
)
from .routed_response_delivery_observer import RoutedResponseDeliveryObserver
from .routed_response_emission import RoutedResponseEmission
from .routed_response_request import RoutedResponseRequest
from .runtime import RoutedResponseMultiSinkDelivery


class RoutedResponseSinkDelivery:
    def __init__(
        self,
        *,
        begin_generation_callback,
        build_output_payload_callback,
        send_output_callback,
        send_full_output_callback,
        remember_history_callback=None,
        ui_presentation_callback=None,
        observer,
    ) -> None:
        if not callable(begin_generation_callback):
            raise TypeError("begin_generation_callback must be callable")
        if type(observer) is not RoutedResponseDeliveryObserver:
            raise TypeError("observer must be an exact RoutedResponseDeliveryObserver")
        self._components = RoutedResponseSinkComponentGraph(
            build_output_payload_callback=build_output_payload_callback,
            send_output_callback=send_output_callback,
            send_full_output_callback=send_full_output_callback,
            remember_history_callback=remember_history_callback,
            ui_presentation_callback=ui_presentation_callback,
            observer=observer,
        )
        self._runtime = RoutedResponseMultiSinkDelivery(
            begin_generation_callback=begin_generation_callback,
            observer=observer,
            sink_components=self._components,
        )

    @property
    def _output_delivery(self):
        return self._components.output_delivery

    @property
    def _full_output_delivery(self):
        return self._components.full_output_delivery

    @property
    def _history_delivery(self):
        return self._components.history_delivery

    @property
    def _ui_delivery(self):
        return self._components.ui_delivery

    def deliver(self, request: RoutedResponseRequest) -> RoutedResponseEmission:
        return self._runtime.deliver(request)


__all__ = ("RoutedResponseSinkDelivery",)
