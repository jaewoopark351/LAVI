#20260905_kpopmodder: Isolate trusted item-command rejection ownership.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.ownership import ItemCommandOwnership


class OrdinaryItemTranslationRejectionOwner:
    _OWNED_STATUSES = frozenset(("unknown", "ambiguous", "unsupported"))

    def __init__(
        self,
        *,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
    ) -> None:
        self._decision_factory = decision_factory
        self._item_command_ownership_classifier = (
            item_command_ownership_classifier
        )
        self._item_command_rejection_evidence_parser = (
            item_command_rejection_evidence_parser
        )
        self._live_proof_validator = live_proof_validator

    def owns_status(self, translation_status: str) -> bool:
        return translation_status in self._OWNED_STATUSES

    def decide(
        self,
        *,
        event: object,
        command_text: str,
        translation: object,
        translation_status: str,
        korean_eligibility_proof: object,
    ) -> MinecraftChatClefInputRouteDecision:
        evidence = self._item_command_rejection_evidence_parser.parse(
            command_text=command_text,
            translation=translation,
            trusted_scope_live=self._live_proof_validator(
                korean_eligibility_proof,
                event,
            ),
        )
        ownership = (
            self._item_command_ownership_classifier.classify_translation_rejection(
                evidence
            )
        )
        if ownership.ownership is ItemCommandOwnership.OWNED_INVALID:
            return self._decision_factory.item_command_translation_rejection(
                translation,
                ownership.reason_code,
                ownership.message,
            )
        return MinecraftChatClefInputRouteDecision.not_handled(
            f"{translation_status}_intent"
        )


__all__ = ("OrdinaryItemTranslationRejectionOwner",)
