#20260905_kpopmodder: Own routed-response Chat UI delivery diagnostics.
from __future__ import annotations


class RoutedResponseChatUiDeliveryDiagnostics:
    def __init__(self, publication_coordinator):
        self._publication_coordinator = publication_coordinator

    def log(
        self,
        emission: object,
        *,
        delivered: bool,
        reason: str,
    ) -> bool:
        return self._publication_coordinator.log_chat_ui_delivery(
            emission,
            delivered=delivered,
            reason=reason,
        )


__all__ = ("RoutedResponseChatUiDeliveryDiagnostics",)
