#20260905_kpopmodder: Own Chat UI delivery observation at routed-response yield.
from __future__ import annotations

from dataclasses import replace

from llm_core.routed_response import RoutedResponseEmission
from llm_core.routed_response.presentation import (
    RoutedResponsePresentationReceipt,
)
from llm_core.routed_response.presentation.ui import (
    RoutedResponseUiPresentationAdapter,
    RoutedResponseUiPresentationIdentity,
)


class RoutedInputChatUiYieldAdapter:
    def __init__(self, response_publisher_callback):
        if not callable(response_publisher_callback):
            raise TypeError("response_publisher_callback must be callable")
        self._response_publisher_callback = response_publisher_callback
        self._ui_adapter = RoutedResponseUiPresentationAdapter()

    def adapt(self, message, response: object) -> object:
        if type(response) is not RoutedResponseEmission:
            return response
        if getattr(message, "source", None) == "lavi_chat_ui":
            if response.presentation_metadata is not None:
                try:
                    presentation_identity = (
                        RoutedResponseUiPresentationIdentity.from_response(
                            event_id=response.event_id,
                            route_kind=response.route_kind,
                            response_kind=response.response_kind,
                            response_source=response.source,
                            source_kind=(
                                response.presentation_metadata.source_kind
                            ),
                            badge_label=(
                                response.presentation_metadata.badge_label
                            ),
                        )
                    )
                    rendered = self._ui_adapter.render(
                        response.text,
                        response.presentation_metadata,
                        presentation_identity=presentation_identity,
                    )
                except Exception:
                    failed = self._with_presentation_receipt(
                        response,
                        accepted=False,
                        reason="delivery_failed",
                    )
                    self._response_publisher_callback().log_chat_ui_delivery(
                        failed,
                        delivered=False,
                        reason="delivery_failed",
                    )
                    return response.text
                delivered = self._with_presentation_receipt(
                    response,
                    accepted=True,
                    reason="yielded",
                )
                self._response_publisher_callback().log_chat_ui_delivery(
                    delivered,
                    delivered=True,
                    reason="yielded",
                )
                return rendered
            self._response_publisher_callback().log_chat_ui_delivery(
                response,
                delivered=True,
                reason="yielded",
            )
        return response.text

    @staticmethod
    def _with_presentation_receipt(
        response: RoutedResponseEmission,
        *,
        accepted: bool,
        reason: str,
    ) -> RoutedResponseEmission:
        return replace(
            response,
            presentation_receipt=RoutedResponsePresentationReceipt(
                event_id=response.event_id,
                sink=RoutedResponseUiPresentationAdapter.SINK,
                accepted=accepted,
                reason=reason,
            ),
        )


__all__ = ("RoutedInputChatUiYieldAdapter",)
