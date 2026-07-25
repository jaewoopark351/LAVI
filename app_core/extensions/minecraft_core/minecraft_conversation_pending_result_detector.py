#20260725_kpopmodder: Added detector for Minecraft actions that outlive the first completion wait.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftConversationPendingResultDetector:
    PENDING_ERRORS = {
        "action_completion_pending",
        "action_completion_watch_timeout",
    }
    PENDING_COMPLETION_STATUSES = {
        "timeout",
        "watch_timeout",
    }

    def is_pending(self, result: Any) -> bool:
        if not isinstance(result, Mapping):
            return False
        error = str(result.get("error") or "").strip()
        if error in self.PENDING_ERRORS:
            return True

        completion = result.get("completion")
        if not isinstance(completion, Mapping):
            return False
        status = str(completion.get("completion_status") or "").strip().lower()
        return status in self.PENDING_COMPLETION_STATUSES
