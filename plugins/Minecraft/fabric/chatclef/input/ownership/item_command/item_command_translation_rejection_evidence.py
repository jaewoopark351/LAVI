#20260905_kpopmodder: Carry parsed rejection evidence without owning routing or rendering.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ItemCommandTranslationRejectionEvidence:
    trusted_scope_live: bool
    status: str
    reason_code: str
    intent_type: str
    item_phrase: str
    resolver_status: str
    resolver_reason_code: str
    resolver_family: str = ""
    explicit_minecraft_marker: bool = False

    @property
    def has_resolver_family(self) -> bool:
        return bool(self.resolver_family)

    @property
    def has_ownership_evidence(self) -> bool:
        return self.has_resolver_family or self.explicit_minecraft_marker


__all__ = ("ItemCommandTranslationRejectionEvidence",)
