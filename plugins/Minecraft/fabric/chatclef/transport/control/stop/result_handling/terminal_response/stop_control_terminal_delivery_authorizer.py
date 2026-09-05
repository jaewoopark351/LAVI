#20260905_kpopmodder: Isolate the one-time STOP terminal delivery authorization.
from __future__ import annotations


class StopControlTerminalDeliveryAuthorizer:
    def claim(self, tracker: object) -> bool:
        return tracker.claim_terminal_delivery() is True


__all__ = ("StopControlTerminalDeliveryAuthorizer",)
