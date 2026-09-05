#20260905_kpopmodder: Own trusted and fallthrough route callback invocation.
from __future__ import annotations


class TrustedKoreanRouteInvoker:
    def __init__(self, route_callback):
        self._route_callback = route_callback

    def route_fallthrough(self, event: object):
        return self._route_callback(event)

    def route_trusted(self, event: object, proof: object):
        return self._route_callback(
            event,
            korean_eligibility_proof=proof,
        )


__all__ = ("TrustedKoreanRouteInvoker",)
