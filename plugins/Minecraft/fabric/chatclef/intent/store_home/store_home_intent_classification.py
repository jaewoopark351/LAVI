#20260827_kpopmodder: Keep STORE_HOME decisions and guarded no-submit metadata typed.
from __future__ import annotations

from dataclasses import dataclass
from types import MappingProxyType
from typing import Any

from plugins.Minecraft.fabric.chatclef.intent.store_home.store_home_intent_decision import (
    StoreHomeIntentDecision,
)


_REJECTION_DETAILS = MappingProxyType(
    {
        StoreHomeIntentDecision.AMBIGUOUS: (
            "store_home_ambiguous",
            "집 보관 요청인지 확실하지 않아서 저장하지 않았어요. "
            '"인벤토리 전부 집에 보관해"처럼 말해 주세요.',
        ),
        StoreHomeIntentDecision.NEGATED: (
            "store_home_negated",
            "집 보관을 하지 말라는 요청으로 판단해 저장하지 않았어요.",
        ),
        StoreHomeIntentDecision.DEFERRED: (
            "store_home_deferred",
            "예약 실행은 지원하지 않아 집 보관 명령을 보내지 않았어요.",
        ),
        StoreHomeIntentDecision.QUESTION: (
            "store_home_question",
            "집 보관에 관한 질문으로 판단해 저장 명령을 보내지 않았어요.",
        ),
        StoreHomeIntentDecision.AMBIGUOUS_COMPOUND: (
            "store_home_ambiguous_compound",
            "여러 마인크래프트 행동이 함께 있어 어느 것도 실행하지 않았어요.",
        ),
    }
)


@dataclass(frozen=True)
class StoreHomeIntentClassification:
    decision: StoreHomeIntentDecision

    GUARD_SOURCE = "rule_store_home_guard"
    GUARD_SLOT = "store_home_guarded"
    DECISION_SLOT = "store_home_decision"
    REASON_SLOT = "reason_code"
    MESSAGE_SLOT = "message"

    @property
    def candidate(self) -> bool:
        return self.decision is not StoreHomeIntentDecision.NO_MATCH

    @property
    def executable(self) -> bool:
        return self.decision is StoreHomeIntentDecision.STORE_HOME

    @property
    def reason_code(self) -> str:
        details = _REJECTION_DETAILS.get(self.decision)
        return "" if details is None else details[0]

    @property
    def message(self) -> str:
        details = _REJECTION_DETAILS.get(self.decision)
        return "" if details is None else details[1]

    def to_guard_slots(self) -> dict[str, Any]:
        if not self.candidate or self.executable:
            return {}
        return {
            self.GUARD_SLOT: True,
            self.DECISION_SLOT: self.decision.value,
            self.REASON_SLOT: self.reason_code,
            self.MESSAGE_SLOT: self.message,
        }

    @classmethod
    def guarded_from_intent(
        cls,
        intent: object,
    ) -> "StoreHomeIntentClassification | None":
        if str(getattr(intent, "source", "")) != cls.GUARD_SOURCE:
            return None
        slots = getattr(intent, "slots", None)
        if not isinstance(slots, dict) or slots.get(cls.GUARD_SLOT) is not True:
            return None
        try:
            decision = StoreHomeIntentDecision(slots.get(cls.DECISION_SLOT))
        except (TypeError, ValueError):
            return None
        classification = cls(decision)
        if not classification.candidate or classification.executable:
            return None
        return classification
