#20260905_kpopmodder: Project generic-crafting admission rejections only.
from __future__ import annotations

from types import MappingProxyType

from ..generic_crafting_defaults_admission_decision import (
    GenericCraftingDefaultsAdmissionDecision,
)


class GenericCraftingAdmissionRejectionDecisionFactory:
    _MESSAGES = MappingProxyType(
        {
            "generic_crafting_activation_duplicate": (
                "이 제작 요청은 이미 처리되었거나 종료되었어요."
            ),
            "generic_crafting_activation_capacity_exhausted": (
                "동시에 처리 중인 제작 요청이 너무 많아요."
            ),
        }
    )
    _DEFAULT_MESSAGE = "제작 요청의 입력 권한을 안전하게 확인하지 못했어요."

    def create(self, reason: str) -> GenericCraftingDefaultsAdmissionDecision:
        return GenericCraftingDefaultsAdmissionDecision(
            admitted=False,
            feature_owned=(
                reason != "generic_crafting_eligibility_proof_invalid"
            ),
            reason_code=reason,
            message=self._MESSAGES.get(reason, self._DEFAULT_MESSAGE),
        )


__all__ = ("GenericCraftingAdmissionRejectionDecisionFactory",)
