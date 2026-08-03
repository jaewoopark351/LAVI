#20260803_kpopmodder: Gate Korean Minecraft-like input before ChatClef translation.
from __future__ import annotations

import re


class MinecraftChatClefInputIntentGate:
    TRIGGER_TERMS = (
        "\uac00\uc838\uc640",
        "\uac00\uc838\uc640\uc918",
        "\uac00\uc838\ub2e4\uc918",
        "\uac00\uc838",
        "\uad6c\ud574",
        "\uad6c\ud574\uc918",
        "\ub9cc\ub4e4\uc5b4",
        "\ub9cc\ub4e4\uc5b4\uc918",
        "\uc81c\uc791",
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

    def should_consider(self, text: object) -> bool:
        normalized = self._normalize(text)
        if not normalized:
            return False
        return any(self._normalize(term) in normalized for term in self.TRIGGER_TERMS)

    def _normalize(self, text: object) -> str:
        return re.sub(r"\s+", "", str(text or "").strip().lower())
