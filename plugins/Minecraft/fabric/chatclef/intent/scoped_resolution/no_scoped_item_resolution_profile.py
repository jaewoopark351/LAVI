#20260905_kpopmodder: Keep the default translation path explicitly free of request-local item aliases.
from __future__ import annotations

from .scoped_item_resolution import ScopedItemResolution


class NoScopedItemResolutionProfile:
    __slots__ = ()

    def __setattr__(self, name: str, value: object) -> None:
        raise TypeError("the no-profile resolution policy is immutable")

    def resolve_exact(self, item_phrase: object) -> ScopedItemResolution | None:
        return None


NO_PROFILE = NoScopedItemResolutionProfile()


__all__ = ("NO_PROFILE", "NoScopedItemResolutionProfile")
