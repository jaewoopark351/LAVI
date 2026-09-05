#20260905_kpopmodder: Isolate extension translation invocation.
from __future__ import annotations

from typing import Any


class MinecraftChatClefTranslationInvoker:
    def invoke(self, extension: Any, text: str) -> Any:
        translator = getattr(extension, "translate_natural_language_command")
        return translator(text)

    def invoke_generic_crafting_defaults(
        self,
        extension: Any,
        text: str,
        *,
        input_event: object,
        item_resolution_profile: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> Any:
        translator = getattr(
            extension,
            "translate_generic_crafting_defaults_command",
        )
        return translator(
            text,
            input_event=input_event,
            item_resolution_profile=item_resolution_profile,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("MinecraftChatClefTranslationInvoker",)
