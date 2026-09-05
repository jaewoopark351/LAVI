#20260905_kpopmodder: Isolate generic-crafting dispatch cleanup invocation.
from __future__ import annotations


class MinecraftGenericCraftingDispatchCleanup:
    def __init__(self, route_owner):
        self._route_owner = route_owner

    def close(self, korean_eligibility_proof: object) -> None:
        close_dispatch = getattr(self._route_owner, "close_dispatch", None)
        if callable(close_dispatch):
            close_dispatch(korean_eligibility_proof)


__all__ = ("MinecraftGenericCraftingDispatchCleanup",)
