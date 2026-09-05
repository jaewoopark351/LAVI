#20260905_kpopmodder: Guard submission with one activation spend only.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class GenericCraftingSubmissionActivationSpendGuard:
    def __init__(self, *, activation_lifecycle, result_factory):
        self._activation_lifecycle = activation_lifecycle
        self._result_factory = result_factory

    def create(
        self,
        command: Any,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ):
        def spend_before_submit(
            request: Any,
            submitted_translation: ChatClefTranslationResultDTO,
        ) -> dict[str, Any] | None:
            if self._activation_lifecycle.spend(
                activation_receipt,
                korean_eligibility_proof,
                request,
                submitted_translation,
            ):
                return None
            return self._result_factory.generic_crafting_defaults_rejection(
                command,
                "generic_crafting_activation_invalid",
                "제작 요청 권한이 제출 직전에 일치하지 않았어요.",
            )

        return spend_before_submit


__all__ = ("GenericCraftingSubmissionActivationSpendGuard",)
