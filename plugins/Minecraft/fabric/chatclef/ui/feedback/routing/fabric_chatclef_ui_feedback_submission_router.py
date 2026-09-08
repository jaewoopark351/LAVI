#20260907_kpopmodder: Bind one direct GUI callback to optional feedback-aware extension APIs.
from __future__ import annotations

from typing import Mapping

from ..provenance import CommandFeedbackUiEventReceiptAuthority


class FabricChatClefUiFeedbackSubmissionRouter:
    RAW_HANDLER = "handle_ui_command_with_feedback"
    RAW_FALLBACK = "handle_command"
    KOREAN_HANDLER = "handle_ui_natural_language_command_with_feedback"
    KOREAN_FALLBACK = "handle_natural_language_command"

    def __init__(self, *, receipt_authority=None) -> None:
        self._receipt_authority = (
            receipt_authority or CommandFeedbackUiEventReceiptAuthority()
        )

    def can_submit_raw(self, extension: object) -> bool:
        return self._has_handler(extension, self.RAW_HANDLER, self.RAW_FALLBACK)

    def can_submit_korean(self, extension: object) -> bool:
        return self._has_handler(
            extension,
            self.KOREAN_HANDLER,
            self.KOREAN_FALLBACK,
        )

    def submit_raw(self, extension: object, request: Mapping[str, object]):
        return self._submit(
            extension,
            request,
            text_key="command",
            feedback_handler_name=self.RAW_HANDLER,
            fallback_handler_name=self.RAW_FALLBACK,
        )

    def submit_korean(self, extension: object, request: Mapping[str, object]):
        return self._submit(
            extension,
            request,
            text_key="text",
            feedback_handler_name=self.KOREAN_HANDLER,
            fallback_handler_name=self.KOREAN_FALLBACK,
        )

    def _submit(
        self,
        extension: object,
        request: Mapping[str, object],
        *,
        text_key: str,
        feedback_handler_name: str,
        fallback_handler_name: str,
    ):
        feedback_handler = getattr(extension, feedback_handler_name, None)
        if callable(feedback_handler):
            receipt = self._receipt_authority.issue(
                request_id=request.get("request_id"),
                source=request.get("source"),
                text=request.get(text_key),
            )
            event = self._receipt_authority.consume(
                receipt,
                request_id=request.get("request_id"),
                source=request.get("source"),
                text=request.get(text_key),
            )
            if event is not None:
                return feedback_handler(request, input_event=event)
        fallback = getattr(extension, fallback_handler_name, None)
        if not callable(fallback):
            raise AttributeError(f"{fallback_handler_name} is unavailable")
        return fallback(request)

    @staticmethod
    def _has_handler(extension: object, *names: str) -> bool:
        return any(callable(getattr(extension, name, None)) for name in names)


__all__ = ("FabricChatClefUiFeedbackSubmissionRouter",)
