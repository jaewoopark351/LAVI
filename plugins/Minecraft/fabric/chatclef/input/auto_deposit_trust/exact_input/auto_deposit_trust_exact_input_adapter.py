#20260905_kpopmodder: Canonicalize only two trusted whole-string @ H5 forms.
from __future__ import annotations

from .auto_deposit_trust_exact_input_adaptation import (
    AutoDepositTrustExactInputAdaptation,
)


class AutoDepositTrustExactInputAdapter:
    CANONICAL_COMMAND = "auto_deposit_trust area 16x16"
    _ALLOWED_DIRECT_FORMS = frozenset(
        {
            "@auto_deposit_trust area 16x16",
            "@auto_deposit_trust 반경 16x16",
        }
    )

    def adapt(self, text: str) -> AutoDepositTrustExactInputAdaptation:
        original = text
        outer_trimmed = text.strip(" ")
        translation_input = (
            self.CANONICAL_COMMAND
            if outer_trimmed in self._ALLOWED_DIRECT_FORMS
            else outer_trimmed
        )
        return AutoDepositTrustExactInputAdaptation(
            original_text=original,
            translation_input_text=translation_input,
        )
