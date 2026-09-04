#20260905_kpopmodder: Reject raw control characters before any H5 trim or normalization.
from __future__ import annotations

import unicodedata


class AutoDepositTrustRawInputSafety:
    def is_safe(self, text: object) -> bool:
        if type(text) is not str:
            return False
        return not any(
            character in {"\r", "\n", "\t"}
            or unicodedata.category(character) in {"Cc", "Cf", "Zl", "Zp"}
            or (
                unicodedata.category(character) == "Zs"
                and character != " "
            )
            for character in text
        )
