#20260905_kpopmodder: Validate an executable request against its bound crafting projection.
from __future__ import annotations

from collections.abc import Mapping
from typing import Any

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from ...generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)


class GenericCraftingActivationRequestValidator:
    def matches(
        self,
        receipt: GenericCraftingDefaultsActivationReceipt,
        request: object,
        translation: Any,
        projection: GenericCraftingDefaultsTranslationProjection,
    ) -> bool:
        request_id = self.value(request, "request_id")
        metadata = self.value(request, "metadata")
        if (
            type(request_id) is not str
            or not request_id
            or self.value(request, "source") != receipt.source
            or self.value(request, "command") != projection.command
            or not isinstance(metadata, Mapping)
        ):
            return False
        input_event = metadata.get("input_event")
        natural_language = metadata.get("natural_language")
        if not isinstance(input_event, Mapping) or not isinstance(
            natural_language,
            Mapping,
        ):
            return False
        if (
            input_event.get("event_id") != receipt.event_id
            or input_event.get("source") != receipt.source
            or input_event.get("provider_id") != receipt.provider_id
            or input_event.get("event_kind") != receipt.event_kind
            or input_event.get("final") is not receipt.final
            or natural_language.get("raw_event_text") != receipt.raw_event_text
            or natural_language.get("original_text")
            != receipt.translation_input_text
            or natural_language.get("translation_input_text")
            != receipt.translation_input_text
        ):
            return False
        try:
            nested_projection = (
                GenericCraftingDefaultsTranslationProjection.from_value(
                    natural_language.get("translation")
                )
            )
            provided_projection = (
                GenericCraftingDefaultsTranslationProjection.from_value(
                    translation
                )
            )
        except Exception:
            return False
        return nested_projection == projection == provided_projection

    def value(self, request: object, name: str) -> object:
        if isinstance(request, Mapping):
            return request.get(name)
        return getattr(request, name, None)


__all__ = ("GenericCraftingActivationRequestValidator",)
