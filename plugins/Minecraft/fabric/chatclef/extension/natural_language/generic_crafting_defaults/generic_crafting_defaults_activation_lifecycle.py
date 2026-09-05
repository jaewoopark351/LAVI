#20260905_kpopmodder: Isolate Feature-B activation inspection, binding, spend, and cleanup.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)


class GenericCraftingDefaultsActivationLifecycle:
    def __init__(
        self,
        registry: GenericCraftingDefaultsActivationRegistry | None,
    ):
        self._registry = registry

    def inspect_issued(
        self,
        receipt: object,
        eligibility_proof: object,
        input_event: object,
    ) -> bool:
        return bool(
            self._registry is not None
            and self._registry.inspect_issued(
                receipt,
                eligibility_proof,
                input_event,
            )
        )

    def bind_translation(
        self,
        receipt: object,
        eligibility_proof: object,
        input_event: object,
        translation: Any,
    ) -> bool:
        return bool(
            self._registry is not None
            and self._registry.bind_translation(
                receipt,
                eligibility_proof,
                input_event,
                translation,
            )
        )

    def spend(
        self,
        receipt: object,
        eligibility_proof: object,
        request: object,
        translation: Any,
    ) -> bool:
        return bool(
            self._registry is not None
            and self._registry.spend(
                receipt,
                eligibility_proof,
                request,
                translation,
            )
        )

    def abandon_if_live(self, receipt: object) -> None:
        if self._registry is None:
            return
        try:
            self._registry.abandon_if_live(receipt)
        except Exception:
            return
