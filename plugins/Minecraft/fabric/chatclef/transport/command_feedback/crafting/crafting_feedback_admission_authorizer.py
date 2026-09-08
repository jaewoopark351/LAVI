#20260907_kpopmodder: Admit only the exact trusted Korean diamond-pickaxe craft request.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)

from .crafting_feedback_admission_grant import CraftingFeedbackAdmissionGrant


class CraftingFeedbackAdmissionAuthorizer:
    COMMAND = "get diamond_pickaxe 1"
    TARGET_ITEM = "diamond_pickaxe"
    REQUESTED_COUNT = 1
    SPOKEN_ITEM_LABEL = "다이아 곡괭이"

    def __init__(self, *, live_proof_validator, verb_matcher=None) -> None:
        self._live_proof_validator = live_proof_validator
        self._verb_matcher = verb_matcher or KoreanAcquisitionVerbMatcher()

    def issue(
        self,
        *,
        event: object,
        korean_eligibility_proof: object,
        translation: object,
    ) -> CraftingFeedbackAdmissionGrant | None:
        if self._live_proof_validator(
            korean_eligibility_proof,
            event,
        ) is not True:
            return None
        if not isinstance(translation, Mapping):
            return None
        intent = translation.get("intent")
        if not isinstance(intent, Mapping):
            return None
        if (
            translation.get("status") != "validated"
            or translation.get("executable") is not True
            or translation.get("command") != self.COMMAND
            or translation.get("resolved_target") != self.TARGET_ITEM
            or intent.get("intent_type") != "get_item"
            or type(intent.get("quantity")) is not int
            or intent.get("quantity") != self.REQUESTED_COUNT
            or intent.get("language") != "ko"
        ):
            return None
        event_text = getattr(event, "text", None)
        original_text = intent.get("original_text")
        if (
            type(event_text) is not str
            or type(original_text) is not str
            or self._verb_matcher.classify(event_text) != "craft"
            or self._verb_matcher.classify(original_text) != "craft"
        ):
            return None
        event_id = getattr(event, "event_id", None)
        input_source = getattr(event, "source", None)
        provider_id = getattr(event, "provider_id", None)
        event_kind = getattr(event, "event_kind", None)
        if not all(
            type(value) is str and bool(value)
            for value in (event_id, input_source, provider_id, event_kind)
        ):
            return None
        return CraftingFeedbackAdmissionGrant._issue(
            acquisition_verb_class="craft",
            command=self.COMMAND,
            command_source=input_source,
            event_id=event_id,
            event_kind=event_kind,
            input_source=input_source,
            provider_id=provider_id,
            requested_count=self.REQUESTED_COUNT,
            spoken_item_label=self.SPOKEN_ITEM_LABEL,
            target_item=self.TARGET_ITEM,
            intent_kind="get_item",
        )


__all__ = ("CraftingFeedbackAdmissionAuthorizer",)
