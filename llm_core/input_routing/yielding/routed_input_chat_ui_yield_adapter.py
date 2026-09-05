#20260905_kpopmodder: Own Chat UI delivery observation at routed-response yield.
from __future__ import annotations

from llm_core.routed_response import RoutedResponseEmission


class RoutedInputChatUiYieldAdapter:
    def __init__(self, response_publisher_callback):
        if not callable(response_publisher_callback):
            raise TypeError("response_publisher_callback must be callable")
        self._response_publisher_callback = response_publisher_callback

    def adapt(self, message, response: object) -> object:
        if type(response) is not RoutedResponseEmission:
            return response
        if getattr(message, "source", None) == "lavi_chat_ui":
            self._response_publisher_callback().log_chat_ui_delivery(
                response,
                delivered=True,
                reason="yielded",
            )
        return response.text


__all__ = ("RoutedInputChatUiYieldAdapter",)
