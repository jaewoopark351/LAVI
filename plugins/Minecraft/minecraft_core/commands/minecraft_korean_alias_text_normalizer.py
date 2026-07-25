#20260725_kpopmodder: Added Korean alias text normalization for STT spacing noise.
from __future__ import annotations


class MinecraftKoreanAliasTextNormalizer:
    def normalize(self, text: object) -> str:
        return "".join(str(text or "").strip().lower().split())
