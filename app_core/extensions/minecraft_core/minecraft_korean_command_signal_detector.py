#20260725_kpopmodder: Added Korean command signal detection for Minecraft conversation routing.
from __future__ import annotations

import re


class MinecraftKoreanCommandSignalDetector:
    COORDINATE_PATTERN = re.compile(r"-?\d+")
    COMMAND_WORDS = (
        "\uba48\ucdb0",
        "\uba48\ucd94",
        "\uc911\uc9c0",
        "\uc815\uc9c0",
        "\ucde8\uc18c",
        "\uc778\ubca4\ud1a0\ub9ac",
        "\uc778\ubca4",
        "\uc0c1\ud0dc",
        "\uccb4\ub825",
        "\ud604\uc7ac \ud589\ub3d9",
        "\ud604\uc7ac \uc561\uc158",
        "\uce90\uc640",
        "\uce90",
        "\uad6c\ud574",
        "\uac00\uc838\uc640",
        "\uc5bb\uc5b4",
        "\ubaa8\uc544",
        "\uc218\uc9d1",
        "\ub9cc\ub4e4",
        "\uc81c\uc791",
        "\uc870\ud569",
        "\uc7a5\ucc29",
        "\ub4e4\uc5b4",
        "\ub4e4\uace0",
        "\ucc29\uc6a9",
        "\uc774\ub3d9",
        "\uc88c\ud45c",
    )

    def looks_like_command(self, text: object) -> bool:
        lowered = str(text or "").strip().lower()
        if not lowered:
            return False
        if any(word in lowered for word in self.COMMAND_WORDS):
            return True
        return len(self.COORDINATE_PATTERN.findall(lowered)) >= 3
