#20260905_kpopmodder: Validate activation receipt authority and exact registry record binding.
from __future__ import annotations

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)


class GenericCraftingActivationRegistryRecordValidator:
    def receipt_belongs_to_registry(
        self,
        receipt: object,
        registry_token: object,
    ) -> bool:
        return bool(
            type(receipt) is GenericCraftingDefaultsActivationReceipt
            and getattr(receipt, "_registry_token", None) is registry_token
        )

    def record_matches_receipt(
        self,
        record: dict[str, object],
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return bool(
            record.get("receipt") is receipt
            and record.get("nonce") is getattr(receipt, "_nonce", None)
        )


__all__ = ("GenericCraftingActivationRegistryRecordValidator",)
