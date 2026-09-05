#20260905_kpopmodder: Commit or reject scoped translation binding only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class GenericCraftingTranslationBindingCommit:
    def __init__(self, *, activation_lifecycle, result_factory):
        self._activation_lifecycle = activation_lifecycle
        self._result_factory = result_factory

    def commit(
        self,
        translated: ChatClefTranslationResultDTO,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
        input_event: object,
    ) -> dict[str, object]:
        if not translated.executable:
            return translated.to_dict()
        if self._activation_lifecycle.bind_translation(
            activation_receipt,
            korean_eligibility_proof,
            input_event,
            translated,
        ):
            return translated.to_dict()
        return self._result_factory.rejected(
            "generic_crafting_translation_binding_invalid",
            "제작 명령의 해석 결과가 요청 권한과 일치하지 않아요.",
        )


__all__ = ("GenericCraftingTranslationBindingCommit",)
