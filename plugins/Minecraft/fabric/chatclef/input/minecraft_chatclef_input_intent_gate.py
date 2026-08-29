#20260803_kpopmodder: Gate Korean Minecraft-like input before ChatClef translation.
#20260827_kpopmodder: Reuse the STORE_HOME rule owner for candidate detection only.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)
from plugins.Minecraft.fabric.chatclef.intent.store_home import (
    KoreanStoreHomeIntentClassifier,
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
        "\uc0c1\uc790\uc5d0\ub123",
        "\uc0c1\uc790\uc5d0\uc800\uc7a5",
        "\uc0c1\uc790\uc5d0\ubcf4\uad00",
        "\ucc3d\uace0\uc5d0\ub123",
        "\ubcf4\uad00\ud568\uc5d0\ub123",
        "\uc785\uc5b4",
        "\ucc29\uc6a9",
        "\uc7a5\ucc29",
        "\uc5d0\uac8c",
        "\ud55c\ud14c",
        "idle",
    )

    def __init__(
        self,
        acquisition_verbs: KoreanAcquisitionVerbMatcher | None = None,
        store_home: KoreanStoreHomeIntentClassifier | None = None,
    ):
        self._acquisition_verbs = acquisition_verbs or KoreanAcquisitionVerbMatcher()
        self._store_home = store_home or KoreanStoreHomeIntentClassifier()

    def should_consider(self, text: object) -> bool:
        normalized = self._normalize(text)
        if not normalized:
            return False
        if self._store_home.is_candidate(text):
            return True
        if self._acquisition_verbs.matches(text):
            return True
        return any(
            self._normalize(term) in normalized
            for term in self.NON_ACQUISITION_TRIGGER_TERMS
        )

    def _normalize(self, text: object) -> str:
        return re.sub(r"\s+", "", str(text or "").strip().lower())
