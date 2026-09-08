#20260905_kpopmodder: Construct the three independent routed-response sinks.
from __future__ import annotations

from ..routed_response_full_output_sink_delivery import (
    RoutedResponseFullOutputSinkDelivery,
)
from ..routed_response_history_sink_delivery import (
    RoutedResponseHistorySinkDelivery,
)
from ..routed_response_output_sink_delivery import (
    RoutedResponseOutputSinkDelivery,
)
from ..presentation.ui import (
    RoutedResponseUiPresentationAdapter,
    RoutedResponseUiSinkDelivery,
)


class RoutedResponseSinkComponentGraph:
    def __init__(
        self,
        *,
        build_output_payload_callback,
        send_output_callback,
        send_full_output_callback,
        remember_history_callback,
        ui_presentation_callback,
        observer,
    ):
        self.output_delivery = RoutedResponseOutputSinkDelivery(
            build_output_payload_callback=build_output_payload_callback,
            send_output_callback=send_output_callback,
            observer=observer,
        )
        self.full_output_delivery = RoutedResponseFullOutputSinkDelivery(
            send_full_output_callback=send_full_output_callback,
            observer=observer,
        )
        self.history_delivery = RoutedResponseHistorySinkDelivery(
            remember_history_callback=remember_history_callback,
            observer=observer,
        )
        self.ui_delivery = RoutedResponseUiSinkDelivery(
            adapter=RoutedResponseUiPresentationAdapter(),
            presentation_callback=ui_presentation_callback,
            observer=observer,
        )


__all__ = ("RoutedResponseSinkComponentGraph",)
