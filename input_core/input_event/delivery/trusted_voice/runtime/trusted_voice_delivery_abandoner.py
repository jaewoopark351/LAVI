#20260905_kpopmodder: Own bounded registered VoiceInput delivery abandonment.
from __future__ import annotations


class TrustedVoiceDeliveryAbandoner:
    def __init__(self, diagnostics):
        self._diagnostics = diagnostics

    def abandon(self, delivery) -> None:
        try:
            delivery.abandon()
        except Exception:
            self._diagnostics.log_failure("abandonment")


__all__ = ("TrustedVoiceDeliveryAbandoner",)
