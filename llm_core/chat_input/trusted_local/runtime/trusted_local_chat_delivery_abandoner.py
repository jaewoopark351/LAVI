#20260905_kpopmodder: Own registered local-Chat delivery abandonment.
from __future__ import annotations


class TrustedLocalChatDeliveryAbandoner:
    def abandon(self, delivery) -> None:
        delivery.abandon()


__all__ = ("TrustedLocalChatDeliveryAbandoner",)
