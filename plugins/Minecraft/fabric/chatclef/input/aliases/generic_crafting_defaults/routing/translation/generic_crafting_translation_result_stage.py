#20260905_kpopmodder: Validate and classify route translation results only.


class GenericCraftingTranslationResultStage:
    def __init__(self, *, translation_boundary, decision_factory):
        self._translation_boundary = translation_boundary
        self._decision_factory = decision_factory

    def evaluate(self, raw_translation: object):
        translation = self._translation_boundary.validate(raw_translation)
        if self._translation_boundary.status(translation) != "validated":
            rejection = (
                self._decision_factory.generic_crafting_defaults_rejection(
                    str(
                        translation.get("reason_code")
                        or "generic_crafting_translation_rejected"
                    ),
                    str(
                        translation.get("message")
                        or "제작 명령을 안전하게 해석하지 못했어요."
                    ),
                    translation,
                )
            )
            return None, rejection
        return translation, None


__all__ = ("GenericCraftingTranslationResultStage",)
