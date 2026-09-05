#20260905_kpopmodder: Define the input-policy-neutral protocol for one-request item resolution.
from __future__ import annotations

from typing import Protocol

from .scoped_item_resolution import ScopedItemResolution


class ScopedItemResolutionProfile(Protocol):
    def resolve_exact(self, item_phrase: object) -> ScopedItemResolution | None:
        ...
