#20260913_kpopmodder: Fail closed on any malformed GOTO guard without fallback reinterpretation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType

from ..goto_parse_decision import GotoParseDecision
from ..goto_parse_result import GotoParseResult
from .goto_guard_fields import DECISION_SLOT, EXPECTED_SLOTS, GUARDED_REASONS, GUARD_SLOT, GUARD_SOURCE, REASON_SLOT
from .goto_guard_marker_detector import GotoGuardMarkerDetector


class GotoGuardIntentDecoder:
    def __init__(self, marker_detector: GotoGuardMarkerDetector | None = None):
        self._marker_detector = marker_detector or GotoGuardMarkerDetector()

    def decode(self, intent: object) -> GotoParseResult | None:
        if not self._marker_detector.has_marker(intent):
            return None
        if not isinstance(intent, ChatClefIntentDTO):
            return self._invalid()
        if (
            intent.intent_type is not ChatClefIntentType.UNKNOWN
            or intent.source != GUARD_SOURCE
            or set(intent.slots) != EXPECTED_SLOTS
            or intent.slots.get(GUARD_SLOT) is not True
            or intent.quantity is not None
            or intent.item_phrase
            or intent.food_units is not None
            or intent.x is not None
            or intent.y is not None
            or intent.z is not None
            or intent.player_name
        ):
            return self._invalid()
        reason = intent.slots.get(REASON_SLOT)
        if not isinstance(reason, str):
            return self._invalid()
        decision = GUARDED_REASONS.get(reason)
        if decision is None or intent.slots.get(DECISION_SLOT) != decision.value:
            return self._invalid()
        return GotoParseResult(
            decision,
            reason_code=reason,
            message=(
                "좌표 이동 명령이 아니어서 실행하지 않았어."
                if decision is GotoParseDecision.NONCOMMAND
                else "X, Y, Z 좌표 세 개를 다시 알려줘."
            ),
        )

    @staticmethod
    def _invalid() -> GotoParseResult:
        return GotoParseResult(
            GotoParseDecision.CLARIFY,
            reason_code="invalid_goto_guard",
            message="X, Y, Z 좌표 세 개를 다시 알려줘.",
        )
