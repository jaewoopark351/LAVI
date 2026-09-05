#20260905_kpopmodder: Guard scoped translation with a live activation only.


class GenericCraftingTranslationActivationGuard:
    def __init__(self, *, activation_lifecycle, result_factory):
        self._activation_lifecycle = activation_lifecycle
        self._result_factory = result_factory

    def rejection_if_invalid(
        self,
        activation_receipt: object,
        korean_eligibility_proof: object,
        input_event: object,
    ):
        if self._activation_lifecycle.inspect_issued(
            activation_receipt,
            korean_eligibility_proof,
            input_event,
        ):
            return None
        return self._result_factory.rejected(
            "generic_crafting_activation_invalid",
            "제작 요청의 입력 권한을 안전하게 확인하지 못했어요.",
        )


__all__ = ("GenericCraftingTranslationActivationGuard",)
