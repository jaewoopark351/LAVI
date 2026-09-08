#20260905_kpopmodder: Keep routed-response presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Add typed lifecycle delivery metadata without changing legacy output payloads.
from __future__ import annotations

from collections.abc import Mapping

from .routed_response_non_preempting_delivery_policy import (
    RoutedResponseNonPreemptingDeliveryPolicy,
)


class RoutedResponseOutputPayloadAdapter:
    def __init__(self, build_output_payload_callback) -> None:
        if not callable(build_output_payload_callback):
            raise TypeError("build_output_payload_callback must be callable")
        self._build_output_payload_callback = build_output_payload_callback

    def build(self, request, response_generation: object):
        payload = self._build_output_payload_callback(
            request.text,
            response_generation,
        )
        if (
            request.delivery_mode
            == RoutedResponseNonPreemptingDeliveryPolicy.CURRENT_INPUT
            and request.presentation_metadata is None
            and not request.send_ui
        ):
            return payload
        if not isinstance(payload, Mapping):
            raise TypeError(
                "metadata-bearing routed output payload must be a mapping"
            )
        enriched = dict(payload)
        enriched.update(
            {
                "event_id": (
                    request.event_id
                    if type(request.event_id) is str and request.event_id
                    else "none"
                ),
                "source": request.source,
                "route_kind": request.route_kind,
                "response_kind": request.response_kind,
                "delivery_mode": request.delivery_mode,
            }
        )
        if request.presentation_metadata is not None:
            enriched["presentation"] = dict(
                request.presentation_metadata.as_mapping()
            )
        return enriched


__all__ = ("RoutedResponseOutputPayloadAdapter",)
