#20260905_kpopmodder: Represent Feature-B admission without producing final item ownership.
from __future__ import annotations

from dataclasses import dataclass

from .generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)


@dataclass(frozen=True)
class GenericCraftingDefaultsAdmissionDecision:
    admitted: bool
    feature_owned: bool
    reason_code: str = ""
    message: str = ""
    receipt: GenericCraftingDefaultsActivationReceipt | None = None
