#20260905_kpopmodder: Validate activation event and candidate inputs only.
from __future__ import annotations

import re

from ...generic_crafting_defaults_candidate import (
    GenericCraftingDefaultsCandidate,
)
from .generic_crafting_activation_validated_input import (
    GenericCraftingActivationValidatedInput,
)


class GenericCraftingActivationEventCandidateValidator:
    EVENT_ID_RE = re.compile(r"^[0-9a-f]{32}$", re.ASCII)

    def validate(
        self,
        event: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> GenericCraftingActivationValidatedInput | None:
        rule = candidate.rule
        intent = candidate.deterministic_intent
        event_id = getattr(event, "event_id", None)
        raw_text = getattr(event, "text", None)
        source = getattr(event, "source", None)
        provider_id = getattr(event, "provider_id", None)
        event_kind = getattr(event, "event_kind", None)
        final = getattr(event, "final", None)
        if not (
            rule is not None
            and type(event_id) is str
            and self.EVENT_ID_RE.fullmatch(event_id) is not None
            and type(raw_text) is str
            and type(source) is str
            and type(provider_id) is str
            and type(event_kind) is str
            and type(final) is bool
            and type(intent.quantity) is int
            and intent.item_phrase == rule.item_phrase
        ):
            return None
        return GenericCraftingActivationValidatedInput(
            event_id=event_id,
            source=source,
            provider_id=provider_id,
            event_kind=event_kind,
            final=final,
            raw_text=raw_text,
            rule=rule,
            intent=intent,
        )

    def is_valid(
        self,
        event: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> bool:
        return self.validate(event, candidate) is not None


__all__ = ("GenericCraftingActivationEventCandidateValidator",)
