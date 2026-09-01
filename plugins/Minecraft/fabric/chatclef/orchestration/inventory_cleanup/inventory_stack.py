#20260901_kpopmodder: Keep normalized inventory-stack evidence in one contract file.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class InventoryStack:
    item: str
    count: int
    slot: str = ""
    protected: bool = False
    tags: frozenset[str] = field(default_factory=frozenset)

    def __post_init__(self) -> None:
        item = str(self.item or "").strip()
        slot = str(self.slot or "").strip()
        count = self.count
        if type(count) is not int:
            raise TypeError("stack count must be an exact int")
        object.__setattr__(self, "item", item)
        object.__setattr__(self, "slot", slot)
        object.__setattr__(self, "tags", frozenset(str(tag) for tag in self.tags))
