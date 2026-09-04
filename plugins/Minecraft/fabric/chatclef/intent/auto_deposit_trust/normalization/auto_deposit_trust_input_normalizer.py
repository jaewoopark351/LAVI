#20260905_kpopmodder: Normalize only allowlisted H5 ASCII spacing and size spellings.
from __future__ import annotations

import re


class AutoDepositTrustInputNormalizer:
    _FIXED_SIZE_RE = re.compile(r"(?<!\d)16 *(?:x|×|곱하기) *16(?!\d)", re.IGNORECASE)
    _WHITESPACE_RE = re.compile(r" +")

    def normalize(self, value: object) -> str:
        #20260905_kpopmodder: Do not compatibility-fold R2 command text; only the documented ASCII size tokens may execute.
        text = str(value or "")
        text = self._FIXED_SIZE_RE.sub("16x16", text)
        return self._WHITESPACE_RE.sub(" ", text).strip(" ")
