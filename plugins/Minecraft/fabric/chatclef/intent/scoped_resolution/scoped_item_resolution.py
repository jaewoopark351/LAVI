#20260905_kpopmodder: Represent one immutable request-local item resolution without input-policy knowledge.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ScopedItemResolution:
    profile_id: str
    rule_id: str
    item_phrase: str
    target: str

    def __post_init__(self) -> None:
        for field_name in ("profile_id", "rule_id", "item_phrase", "target"):
            value = getattr(self, field_name)
            if type(value) is not str or not value or value != value.strip():
                raise ValueError(f"invalid_scoped_item_resolution_{field_name}")
