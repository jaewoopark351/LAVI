#20260905_kpopmodder: Invoke one request-local crafting translation only.


class GenericCraftingTranslationInvoker:
    def __init__(self, *, extension, translation_boundary, profile):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._profile = profile

    def invoke(
        self,
        *,
        event: object,
        receipt: object,
        korean_eligibility_proof: object,
    ):
        return self._translation_boundary.translate_generic_crafting_defaults_once(
            self._extension,
            receipt.translation_input_text,
            input_event=event,
            item_resolution_profile=self._profile,
            activation_receipt=receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("GenericCraftingTranslationInvoker",)
