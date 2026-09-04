#20260905_kpopmodder: Keep H5 classification and bounded rejection metadata immutable.
from __future__ import annotations

from dataclasses import dataclass
from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.contracts.auto_deposit_trust_intent_decision import (
    AutoDepositTrustIntentDecision,
)


_REJECTION_DETAILS = MappingProxyType(
    {
        AutoDepositTrustIntentDecision.NEGATED: (
            "auto_deposit_trust_area_negated",
            "자동 보관 대상 등록을 하지 말라는 요청으로 판단해 명령을 보내지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.QUESTION: (
            "auto_deposit_trust_area_question",
            "자동 보관 대상 등록에 관한 질문으로 판단해 명령을 보내지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.DEFERRED: (
            "auto_deposit_trust_area_deferred",
            "예약 실행은 지원하지 않아 자동 보관 대상 등록 명령을 보내지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.AMBIGUOUS: (
            "auto_deposit_trust_area_ambiguous",
            "주변 상자를 자동 보관 대상으로 등록하라는 요청인지 확실하지 않아 실행하지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.AMBIGUOUS_COMPOUND: (
            "auto_deposit_trust_area_ambiguous_compound",
            "여러 마인크래프트 행동이 함께 있어 어느 것도 실행하지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE: (
            "auto_deposit_trust_area_unsupported_size",
            "자동 보관 대상 일괄 등록은 정확히 16x16 범위만 지원해 실행하지 않았어요.",
        ),
        AutoDepositTrustIntentDecision.MALFORMED: (
            "auto_deposit_trust_area_malformed",
            "자동 보관 대상 등록 요청 형식을 확인할 수 없어 실행하지 않았어요.",
        ),
    }
)


@dataclass(frozen=True)
class AutoDepositTrustIntentClassification:
    decision: AutoDepositTrustIntentDecision

    def __post_init__(self) -> None:
        object.__setattr__(
            self,
            "decision",
            AutoDepositTrustIntentDecision(self.decision),
        )

    @property
    def candidate(self) -> bool:
        return self.decision is not AutoDepositTrustIntentDecision.NO_MATCH

    @property
    def executable(self) -> bool:
        return (
            self.decision
            is AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA
        )

    @property
    def guarded(self) -> bool:
        return self.candidate and not self.executable

    @property
    def reason_code(self) -> str:
        details = _REJECTION_DETAILS.get(self.decision)
        return "" if details is None else details[0]

    @property
    def message(self) -> str:
        details = _REJECTION_DETAILS.get(self.decision)
        return "" if details is None else details[1]
