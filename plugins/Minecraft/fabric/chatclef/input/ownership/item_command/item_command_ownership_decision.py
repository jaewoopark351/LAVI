#20260905_kpopmodder: Carry one immutable final item-command ownership decision.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_candidate import (
    GenericCraftingDefaultsCandidate,
)

from .item_command_ownership import ItemCommandOwnership


@dataclass(frozen=True)
class ItemCommandOwnershipDecision:
    ownership: ItemCommandOwnership
    reason_code: str
    message: str = ""
    candidate: GenericCraftingDefaultsCandidate | None = None

    @property
    def owned(self) -> bool:
        return self.ownership is not ItemCommandOwnership.UNRELATED
