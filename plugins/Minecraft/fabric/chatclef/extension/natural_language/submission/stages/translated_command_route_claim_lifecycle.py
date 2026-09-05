#20260905_kpopmodder: Isolate translated-command route-claim cleanup.
from __future__ import annotations


class TranslatedCommandRouteClaimLifecycle:
    def __init__(self, admission):
        self._admission = admission

    def abandon_if_issued(self, route_claim: object) -> None:
        try:
            self._admission.abandon_if_issued(route_claim)
        except Exception:
            return


__all__ = ("TranslatedCommandRouteClaimLifecycle",)
