#20260913_kpopmodder: Encode non-executable coordinate decisions for shared translation entrypoints.
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType

from ..goto_parse_result import GotoParseResult
from .goto_guard_fields import DECISION_SLOT, GUARDED_REASONS, GUARD_SLOT, GUARD_SOURCE, REASON_SLOT


class GotoGuardIntentEncoder:
    def encode(self, original: str, result: GotoParseResult) -> ChatClefIntentDTO:
        if GUARDED_REASONS.get(result.reason_code) is not result.decision:
            raise ValueError("goto_guard_requires_canonical_rejection")
        return ChatClefIntentDTO(
            intent_type=ChatClefIntentType.UNKNOWN,
            original_text=original,
            source=GUARD_SOURCE,
            slots={
                GUARD_SLOT: True,
                DECISION_SLOT: result.decision.value,
                REASON_SLOT: result.reason_code,
            },
        )
