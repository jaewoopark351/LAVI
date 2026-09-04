#20260905_kpopmodder: Preserve raw H5 text beside its canonical translation input.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AutoDepositTrustExactInputAdaptation:
    original_text: str
    translation_input_text: str
