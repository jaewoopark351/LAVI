#20260905_kpopmodder: Preserve activation-context creation as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from ..generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate
from .context import (
    GenericCraftingActivationContextProjector,
    GenericCraftingActivationEventCandidateValidator,
)


class GenericCraftingActivationContextFactory:
    EVENT_ID_RE = GenericCraftingActivationEventCandidateValidator.EVENT_ID_RE

    def __init__(
        self,
        normalizer: KoreanTextNormalizer | None = None,
        *,
        input_validator=None,
        context_projector=None,
    ):
        self._input_validator = (
            input_validator
            or GenericCraftingActivationEventCandidateValidator()
        )
        self._context_projector = (
            context_projector
            or GenericCraftingActivationContextProjector(normalizer)
        )
        self.normalizer = self._context_projector.normalizer

    def create(
        self,
        event: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> dict[str, object] | None:
        validated_input = self._input_validator.validate(event, candidate)
        if validated_input is None:
            return None
        return self._context_projector.project(validated_input)


__all__ = ("GenericCraftingActivationContextFactory",)
