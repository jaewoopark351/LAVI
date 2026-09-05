#20260905_kpopmodder: Isolate optional route-owner invocation.
from __future__ import annotations

from collections.abc import Callable


class MinecraftOptionalRouteOwnerCall:
    def invoke(
        self,
        try_route: Callable,
        event: object,
        korean_eligibility_proof: object,
    ) -> object:
        return try_route(event, korean_eligibility_proof)


__all__ = ("MinecraftOptionalRouteOwnerCall",)
