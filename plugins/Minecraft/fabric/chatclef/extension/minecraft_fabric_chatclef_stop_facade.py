#20260905_kpopmodder: Isolate extension-level STOP adapter delegation.
from __future__ import annotations


class MinecraftFabricChatClefStopFacade:
    def __init__(self, adapter):
        self._adapter = adapter

    def submit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ):
        return self._adapter.submit_stop_control(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )

    def claim_registry(self):
        getter = getattr(self._adapter, "get_stop_control_claim_registry", None)
        return getter() if callable(getter) else None

    def set_terminal_response_callback(self, callback) -> None:
        self._adapter.set_stop_terminal_response_callback(callback)


__all__ = ("MinecraftFabricChatClefStopFacade",)
