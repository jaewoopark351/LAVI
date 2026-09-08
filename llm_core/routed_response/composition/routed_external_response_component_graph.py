#20260905_kpopmodder: Assemble routed-response publication and diagnostics components.
from __future__ import annotations

from ..diagnostics import RoutedResponseChatUiDeliveryDiagnostics
from ..routed_response_capability_authorizer import (
    RoutedResponseCapabilityAuthorizer,
)
from ..routed_response_delivery_observer import RoutedResponseDeliveryObserver
from ..routed_response_publication_coordinator import (
    RoutedResponsePublicationCoordinator,
)
from ..routed_response_request_validator import RoutedResponseRequestValidator
from ..routed_response_sink_delivery import RoutedResponseSinkDelivery
from ..runtime import RoutedExternalResponseRuntime


class RoutedExternalResponseComponentGraph:
    def __init__(
        self,
        *,
        begin_generation_callback,
        build_output_payload_callback,
        send_output_callback,
        send_full_output_callback,
        remember_history_callback,
        ui_presentation_callback,
        emission_capability_consumer,
        delivery_logger,
        log_callback,
    ):
        self.delivery_observer = RoutedResponseDeliveryObserver(
            delivery_logger=delivery_logger,
            log_callback=log_callback,
        )
        self.request_validator = RoutedResponseRequestValidator(
            history_available=remember_history_callback is not None,
        )
        self.capability_authorizer = RoutedResponseCapabilityAuthorizer(
            emission_capability_consumer,
        )
        self.sink_delivery = RoutedResponseSinkDelivery(
            begin_generation_callback=begin_generation_callback,
            build_output_payload_callback=build_output_payload_callback,
            send_output_callback=send_output_callback,
            send_full_output_callback=send_full_output_callback,
            remember_history_callback=remember_history_callback,
            ui_presentation_callback=ui_presentation_callback,
            observer=self.delivery_observer,
        )
        self.publication_coordinator = RoutedResponsePublicationCoordinator(
            request_validator=self.request_validator,
            capability_authorizer=self.capability_authorizer,
            sink_delivery=self.sink_delivery,
            delivery_observer=self.delivery_observer,
        )
        self.runtime = RoutedExternalResponseRuntime(
            self.publication_coordinator
        )
        self.chat_ui_diagnostics = RoutedResponseChatUiDeliveryDiagnostics(
            self.publication_coordinator
        )


__all__ = ("RoutedExternalResponseComponentGraph",)
