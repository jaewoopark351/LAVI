#20260905_kpopmodder: Keep H5 guard decode state and metadata immutable.
from __future__ import annotations

from dataclasses import dataclass
from typing import ClassVar

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.contracts import (
    AutoDepositTrustIntentClassification,
)


@dataclass(frozen=True)
class AutoDepositTrustGuardDecodeResult:
    status: str
    classification: AutoDepositTrustIntentClassification | None = None
    reason_code: str = ""
    message: str = ""

    NOT_GUARD: ClassVar[str] = "NOT_GUARD"
    VALID: ClassVar[str] = "VALID"
    INVALID: ClassVar[str] = "INVALID"
    MALFORMED_REASON: ClassVar[str] = "auto_deposit_trust_area_malformed_guard"
    MALFORMED_MESSAGE: ClassVar[str] = (
        "자동 보관 대상 등록 거절 정보를 확인할 수 없어 실행하지 않았어요."
    )

    def __post_init__(self) -> None:
        if self.status not in {self.NOT_GUARD, self.VALID, self.INVALID}:
            raise ValueError("invalid_auto_deposit_trust_guard_decode_status")

    @property
    def is_guard(self) -> bool:
        return self.status != self.NOT_GUARD

    @property
    def is_valid(self) -> bool:
        return self.status == self.VALID

    @property
    def is_invalid(self) -> bool:
        return self.status == self.INVALID

    @classmethod
    def not_guard(cls) -> "AutoDepositTrustGuardDecodeResult":
        return cls(status=cls.NOT_GUARD)

    @classmethod
    def valid(
        cls,
        classification: AutoDepositTrustIntentClassification,
    ) -> "AutoDepositTrustGuardDecodeResult":
        return cls(
            status=cls.VALID,
            classification=classification,
            reason_code=classification.reason_code,
            message=classification.message,
        )

    @classmethod
    def invalid(cls) -> "AutoDepositTrustGuardDecodeResult":
        return cls(
            status=cls.INVALID,
            reason_code=cls.MALFORMED_REASON,
            message=cls.MALFORMED_MESSAGE,
        )
