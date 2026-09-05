#20260905_kpopmodder: Validate an activation event against its exact issued receipt.
from __future__ import annotations

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)


class GenericCraftingActivationEventReceiptValidator:
    def matches(
        self,
        event: object,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return (
            getattr(event, "event_id", None) == receipt.event_id
            and getattr(event, "source", None) == receipt.source
            and getattr(event, "provider_id", None) == receipt.provider_id
            and getattr(event, "event_kind", None) == receipt.event_kind
            and getattr(event, "final", None) is receipt.final
            and getattr(event, "text", None) == receipt.raw_event_text
        )


__all__ = ("GenericCraftingActivationEventReceiptValidator",)
