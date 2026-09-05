#20260905_kpopmodder: Represent the side-effect-free lexical quantity shape for Feature B.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class GenericCraftingQuantityShape:
    valid: bool
    token_count: int
    normalized_text: str
    text_without_tokens: str
    reason_code: str = ""

    def __post_init__(self) -> None:
        if type(self.valid) is not bool:
            raise TypeError("quantity shape valid must be bool")
        if type(self.token_count) is not int or self.token_count < 0:
            raise ValueError("quantity shape token_count must be non-negative")
