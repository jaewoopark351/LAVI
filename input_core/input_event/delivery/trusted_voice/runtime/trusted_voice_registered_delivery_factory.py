#20260905_kpopmodder: Create one registered VoiceInput delivery per callback.
from __future__ import annotations


class TrustedVoiceRegisteredDeliveryFactory:
    def __init__(self, invocation_owner):
        self._invocation_owner = invocation_owner

    def create(self, payload):
        registrar = self._invocation_owner.begin_invocation()
        return registrar.create_registered_delivery(payload)


__all__ = ("TrustedVoiceRegisteredDeliveryFactory",)
