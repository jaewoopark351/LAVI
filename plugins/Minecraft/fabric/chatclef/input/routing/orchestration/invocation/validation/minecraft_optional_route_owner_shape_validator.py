#20260905_kpopmodder: Isolate optional route-owner callable shape validation.
from __future__ import annotations

from collections.abc import Callable


class MinecraftOptionalRouteOwnerShapeValidator:
    def resolve(self, owner: object) -> Callable | None:
        if owner is None:
            return None
        try_route = getattr(owner, "try_route", None)
        if not callable(try_route):
            raise TypeError("optional route owner must provide try_route")
        return try_route


__all__ = ("MinecraftOptionalRouteOwnerShapeValidator",)
