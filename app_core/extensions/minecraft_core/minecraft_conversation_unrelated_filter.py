#20260725_kpopmodder: Added conservative negative filter for implicit Minecraft routing.
from __future__ import annotations


class MinecraftConversationUnrelatedFilter:
    UNRELATED_KEYWORDS = (
        "git",
        "github",
        "openai",
        "python",
        "java",
        "vscode",
        "\uae43",
        "\ucee4\ubc0b",
        "\ube0c\ub79c\uce58",
        "\ub85c\uadf8",
        "\ud14c\uc2a4\ud2b8",
        "\ub9ac\ud329\ud1a0\ub9c1",
        "\ub9ac\ud399\ud1a0\ub9c1",
        "\ucf54\ub4dc",
        "\ud30c\uc774\uc36c",
        "\uc790\ubc14",
        "\ud074\ub798\uc2a4",
        "\ud568\uc218",
        "\ud30c\uc77c",
        "\ud3f4\ub354",
        "\ubb38\uc11c",
        "\uc124\uba85",
        "\ubc29\ubc95",
        "\ub9ac\ubdf0",
        "\ub0a0\uc528",
        "\uc2dc\uac04",
    )
    GENERAL_CHAT_PHRASES = (
        "\uc548\ub155",
        "\uace0\ub9c8\uc6cc",
        "\uc218\uace0",
        "\ubb50\ud574",
        "\uc5b4\ub5a4 \uac78 \ud560\uae4c",
    )

    def is_unrelated(self, text: object) -> bool:
        lowered = str(text or "").strip().lower()
        if not lowered:
            return True
        if any(keyword in lowered for keyword in self.UNRELATED_KEYWORDS):
            return True
        return any(phrase in lowered for phrase in self.GENERAL_CHAT_PHRASES)
