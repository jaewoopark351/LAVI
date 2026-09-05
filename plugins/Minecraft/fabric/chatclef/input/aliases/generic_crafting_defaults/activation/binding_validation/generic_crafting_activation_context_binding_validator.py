#20260905_kpopmodder: Coordinate exact proof, event, and receipt context binding checks.
from __future__ import annotations

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)


class GenericCraftingActivationContextBindingValidator:
    def __init__(self, *, proof_validator: object, event_validator: object):
        self._proof_validator = proof_validator
        self._event_validator = event_validator

    def matches(
        self,
        record: dict[str, object],
        eligibility_proof: object,
        event: object,
    ) -> bool:
        receipt = record.get("receipt")
        return bool(
            record.get("proof") is eligibility_proof
            and record.get("event") is event
            and self._proof_validator.is_valid(eligibility_proof, event)
            and isinstance(receipt, GenericCraftingDefaultsActivationReceipt)
            and self._event_validator.matches(event, receipt)
        )


__all__ = ("GenericCraftingActivationContextBindingValidator",)
