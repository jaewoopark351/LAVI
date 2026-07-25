#20260725_kpopmodder: Added focused builder for Minecraft command failure replies.
from __future__ import annotations


class MinecraftConversationFailureReplyBuilder:
    RESULT_CHECK_FAILURES = {
        "action_completion_failed",
        "inventory_verification_failed",
    }

    def build(self, detail: str) -> str:
        normalized = str(detail or "").strip()
        if normalized == "unknown_action":
            return (
                "\uc74c, \uadf8 \ub9d0\uc740 \uc544\uc9c1 \ub9c8\ud06c \uba85\ub839\uc73c\ub85c "
                "\uc798 \ubabb \uc54c\uc544\ub4e4\uc5c8\uc5b4. \uc544\uc774\ud15c \uc774\ub984\uc774\ub098 "
                "\ud589\ub3d9\uc744 \uc870\uae08\ub9cc \ub354 \ub610\ub837\ud558\uac8c \ub9d0\ud574\uc918."
            )
        if normalized == "missing_item":
            return (
                "\uc5b4\ub5a4 \uc544\uc774\ud15c\uc778\uc9c0 \ubabb \uc7a1\uc558\uc5b4. "
                "\uc544\uc774\ud15c \uc774\ub984\uc744 \ud55c \ubc88\ub9cc \ub354 \ub9d0\ud574\uc918."
            )
        if normalized == "actions_disabled":
            return (
                "\ub9c8\ud06c \ud589\ub3d9 \uad8c\ud55c\uc774 \uaebc\uc838 \uc788\uc5b4\uc11c "
                "\uc9c0\uae08\uc740 \uc2e4\ud589\uc744 \ubabb \ud574."
            )
        if normalized in self.RESULT_CHECK_FAILURES:
            return (
                "\uc2dc\ud0a4\uae34 \ud588\ub294\ub370 \ub05d\uae4c\uc9c0 \ud655\uc778\ud558\ub294 "
                "\ub370\uc11c \ub9c9\ud614\uc5b4. \ub9c8\ud06c \ud654\uba74\uc774\ub098 "
                "\uc778\ubca4\ud1a0\ub9ac \ud55c \ubc88\ub9cc \ubd10\uc904\ub798?"
            )
        return (
            "\ub9c8\ud06c\uc5d0\uc11c \ud574\ubcf4\ub824\ub2e4 \ub9c9\ud614\uc5b4. "
            f"{normalized or 'unknown error'}"
        )
