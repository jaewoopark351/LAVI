#20260905_kpopmodder: Carry one immutable validated activation input snapshot.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class GenericCraftingActivationValidatedInput:
    event_id: str
    source: str
    provider_id: str
    event_kind: str
    final: bool
    raw_text: str
    rule: object
    intent: object


__all__ = ("GenericCraftingActivationValidatedInput",)
