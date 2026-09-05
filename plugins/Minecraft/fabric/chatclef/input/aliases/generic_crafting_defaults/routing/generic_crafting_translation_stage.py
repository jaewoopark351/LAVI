#20260905_kpopmodder: Preserve route translation as a thin facade.
from __future__ import annotations

from .translation import (
    GenericCraftingTranslationInvoker,
    GenericCraftingTranslationResultStage,
)


class GenericCraftingTranslationStage:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        decision_factory,
        profile,
        translation_invoker=None,
        result_stage=None,
    ):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._decision_factory = decision_factory
        self._profile = profile
        self._translation_invoker = (
            translation_invoker
            or GenericCraftingTranslationInvoker(
                extension=extension,
                translation_boundary=translation_boundary,
                profile=profile,
            )
        )
        self._result_stage = (
            result_stage
            or GenericCraftingTranslationResultStage(
                translation_boundary=translation_boundary,
                decision_factory=decision_factory,
            )
        )

    def translate(
        self,
        *,
        event: object,
        receipt: object,
        korean_eligibility_proof: object,
    ):
        raw_translation = self._translation_invoker.invoke(
            event=event,
            receipt=receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )
        return self._result_stage.evaluate(raw_translation)


__all__ = ("GenericCraftingTranslationStage",)
