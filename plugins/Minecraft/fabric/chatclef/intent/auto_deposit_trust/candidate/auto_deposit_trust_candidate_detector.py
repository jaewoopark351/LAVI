#20260905_kpopmodder: Detect coarse H5 candidates without deciding or causing side effects.
from __future__ import annotations

import unicodedata

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.normalization import (
    AutoDepositTrustInputNormalizer,
)


class AutoDepositTrustCandidateDetector:
    _COMMAND_MARKER_KEYS = ("autodeposittrust", "자동보관등록")
    _COMMAND_PREFIX_KEYS = ("autodeposit",)
    _TARGET_TERMS = ("상자", "컨테이너", "보관함", "chest", "container")
    _PURPOSE_TERMS = (
        "자동보관",
        "자동 보관",
        "자동입고",
        "자동 입고",
        "보관대상",
        "보관 대상",
        "입고대상",
        "입고 대상",
        "trusted storage",
    )

    def __init__(
        self,
        normalizer: AutoDepositTrustInputNormalizer | None = None,
    ):
        self._normalizer = normalizer or AutoDepositTrustInputNormalizer()

    def is_candidate(self, text: object) -> bool:
        try:
            raw = str(text or "")
            normalized = self._normalizer.normalize(text)
            candidate_keys = tuple(
                key
                for key in {
                    self._candidate_key(raw),
                    self._candidate_key(normalized),
                    #20260905_kpopmodder: Compatibility folding is detection-only so obfuscated H5 text cannot reach the LLM or become executable.
                    self._candidate_key(unicodedata.normalize("NFKC", raw)),
                }
                if key
            )
            if not candidate_keys:
                return False
            if any(
                marker in key
                for key in candidate_keys
                for marker in self._COMMAND_MARKER_KEYS
            ):
                return True
            if any(
                marker in key
                for key in candidate_keys
                for marker in self._COMMAND_PREFIX_KEYS
            ):
                return True
            return any(
                "등록" in key
                and (
                    any(
                        term.replace(" ", "") in key
                        for term in self._TARGET_TERMS
                    )
                    or any(
                        term.replace(" ", "") in key
                        for term in self._PURPOSE_TERMS
                    )
                )
                for key in candidate_keys
            )
        except Exception:
            return False

    def _candidate_key(self, text: str) -> str:
        return "".join(
            character.lower() if character.isascii() else character
            for character in text
            if (
                character.isascii() and character.isalnum()
            ) or "가" <= character <= "힣"
        )
