#20260905_kpopmodder: Own the item-versus-non-item translation branch decision.
from __future__ import annotations


class ChatClefTranslationBranchSelector:
    def __init__(self, *, item_action_intents: frozenset[object]):
        self._item_action_intents = item_action_intents

    def is_item_action(self, intent: object) -> bool:
        return intent.intent_type in self._item_action_intents


__all__ = ("ChatClefTranslationBranchSelector",)
