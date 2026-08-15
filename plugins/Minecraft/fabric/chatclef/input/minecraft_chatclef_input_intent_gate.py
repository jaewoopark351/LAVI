#20260803_kpopmodder: Gate Korean Minecraft-like input before ChatClef translation.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)


class MinecraftChatClefInputIntentGate:
    NON_ACQUISITION_TRIGGER_TERMS = (
        "\uc774\ub3d9",
        "\uc88c\ud45c",
        "\ub530\ub77c\uac00",
        "\ub530\ub77c\uc640",
        "\ucad3\uc544\uac00",
        "\uba48\ucdb0",
        "\uc911\uc9c0",
        "\uc815\uc9c0",
        "\uc2a4\ud1b1",
        "\uadf8\ub9cc",
        "\uac00\ub9cc\ud788",
        "\ub300\uae30",
        "idle",
    )

    def __init__(
        self,
        acquisition_verbs: KoreanAcquisitionVerbMatcher | None = None,
    ):
        self._acquisition_verbs = acquisition_verbs or KoreanAcquisitionVerbMatcher()

    def should_consider(self, text: object) -> bool:
        normalized = self._normalize(text)
        if not normalized:
            return False
        if self._acquisition_verbs.matches(text):
            return True
        return any(
            self._normalize(term) in normalized
            for term in self.NON_ACQUISITION_TRIGGER_TERMS
        )

    def _normalize(self, text: object) -> str:
        return re.sub(r"\s+", "", str(text or "").strip().lower())
