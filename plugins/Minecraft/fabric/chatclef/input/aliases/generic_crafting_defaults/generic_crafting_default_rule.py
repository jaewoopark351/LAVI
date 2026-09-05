#20260905_kpopmodder: Bind one exact Korean crafting phrase to its canonical ChatClef catalog target.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class GenericCraftingDefaultRule:
    rule_id: str
    item_phrase: str
    canonical_target: str

    def __post_init__(self) -> None:
        for field_name in ("rule_id", "item_phrase", "canonical_target"):
            value = getattr(self, field_name)
            if type(value) is not str or not value or value != value.strip():
                raise ValueError(f"invalid_generic_crafting_default_{field_name}")
