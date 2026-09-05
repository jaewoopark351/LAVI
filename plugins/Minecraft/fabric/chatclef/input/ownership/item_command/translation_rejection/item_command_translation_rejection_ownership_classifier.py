#20260905_kpopmodder: Classify trusted translation-rejection ownership only.
from __future__ import annotations

from ..item_command_ownership import ItemCommandOwnership
from ..item_command_ownership_decision import ItemCommandOwnershipDecision
from ..item_command_translation_rejection_evidence import (
    ItemCommandTranslationRejectionEvidence,
)


class ItemCommandTranslationRejectionOwnershipClassifier:
    _OWNED_STATUSES = frozenset({"unknown", "ambiguous", "unsupported"})

    def __init__(self, rejection_renderer):
        self._rejection_renderer = rejection_renderer

    def classify(
        self,
        evidence: ItemCommandTranslationRejectionEvidence,
    ) -> ItemCommandOwnershipDecision:
        if (
            type(evidence) is not ItemCommandTranslationRejectionEvidence
            or not evidence.trusted_scope_live
            or evidence.status not in self._OWNED_STATUSES
            or not evidence.has_ownership_evidence
        ):
            return ItemCommandOwnershipDecision(
                ItemCommandOwnership.UNRELATED,
                "item_command_translation_rejection_unrelated",
            )
        return ItemCommandOwnershipDecision(
            ItemCommandOwnership.OWNED_INVALID,
            evidence.reason_code or evidence.resolver_reason_code,
            self._rejection_renderer.render(evidence.status),
        )


__all__ = ("ItemCommandTranslationRejectionOwnershipClassifier",)
