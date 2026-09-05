#20260905_kpopmodder: Create one registered local-Chat delivery per callback invocation.
from __future__ import annotations

from .trusted_local_chat_delivery_attempt import TrustedLocalChatDeliveryAttempt


class TrustedLocalChatRegisteredDeliveryFactory:
    def __init__(self, invocation_owner):
        self._invocation_owner = invocation_owner

    def create(self, message) -> TrustedLocalChatDeliveryAttempt:
        registrar = self._invocation_owner.begin_invocation()
        return TrustedLocalChatDeliveryAttempt(
            registrar=registrar,
            delivery=registrar.create_registered_delivery(message),
        )


__all__ = ("TrustedLocalChatRegisteredDeliveryFactory",)
