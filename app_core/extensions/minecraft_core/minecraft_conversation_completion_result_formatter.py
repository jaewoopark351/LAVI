#20260725_kpopmodder: Added formatter for Minecraft action completion notifications.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_completion_phrase_builder import (
    MinecraftConversationCompletionPhraseBuilder,
)
from .minecraft_conversation_failure_reply_builder import (
    MinecraftConversationFailureReplyBuilder,
)
from .minecraft_conversation_pending_reply_builder import (
    MinecraftConversationPendingReplyBuilder,
)
from .minecraft_conversation_pending_result_detector import (
    MinecraftConversationPendingResultDetector,
)


class MinecraftConversationCompletionResultFormatter:
    def __init__(
        self,
        phrase_builder: MinecraftConversationCompletionPhraseBuilder | None = None,
        failure_reply_builder: MinecraftConversationFailureReplyBuilder | None = None,
        pending_reply_builder: MinecraftConversationPendingReplyBuilder | None = None,
        pending_result_detector: MinecraftConversationPendingResultDetector | None = None,
    ):
        self.phrase_builder = phrase_builder or MinecraftConversationCompletionPhraseBuilder()
        self.failure_reply_builder = (
            failure_reply_builder or MinecraftConversationFailureReplyBuilder()
        )
        self.pending_reply_builder = (
            pending_reply_builder or MinecraftConversationPendingReplyBuilder()
        )
        self.pending_result_detector = (
            pending_result_detector or MinecraftConversationPendingResultDetector()
        )

    def command_failed(
        self,
        route: MinecraftConversationCommandRoute,
        error: Exception,
    ) -> str:
        return self.failure_reply_builder.build(f"{type(error).__name__}: {error}")

    def format(
        self,
        route: MinecraftConversationCommandRoute,
        result: Any,
    ) -> str:
        if not isinstance(result, Mapping):
            return "\ub2e4 \ud588\uc5b4."

        if self.pending_result_detector.is_pending(result):
            return self.pending_reply_builder.build(route, result)

        if self._was_cancelled(result):
            return "\uba48\ucd9c\uc5c8\uc5b4. \ubc29\uae08 \ud558\ub358 \ub9c8\ud06c \uc791\uc5c5\uc740 \ucde8\uc18c\ud588\uc5b4."

        ok = bool(result.get("ok"))
        if not ok:
            detail = str(result.get("error") or result.get("message") or "").strip()
            return self.failure_reply_builder.build(detail)

        phrase = self.phrase_builder.build(route, result)
        return f"\ub2e4 \ud588\uc5b4. {phrase}."

    def _was_cancelled(self, result: Mapping[str, Any]) -> bool:
        action = result.get("action")
        if isinstance(action, Mapping):
            status = str(action.get("status") or "").strip().lower()
            if status == "cancelled":
                return True

        completion = result.get("completion")
        if isinstance(completion, Mapping):
            status = str(
                completion.get("completion_status")
                or completion.get("action_status")
                or ""
            ).strip().lower()
            return status == "cancelled"
        return False
