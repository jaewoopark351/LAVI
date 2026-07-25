#20260725_kpopmodder: Added watcher that follows long Minecraft actions after the first verification timeout.
from __future__ import annotations

import time
from typing import Any, Callable, Mapping

from core.logger import log_print

from .minecraft_conversation_action_id_reader import (
    MinecraftConversationActionIdReader,
)
from .minecraft_conversation_status_action_reader import (
    MinecraftConversationStatusActionReader,
)


class MinecraftConversationPendingActionWatcher:
    TERMINAL_STATUSES = {"succeeded", "failed", "cancelled"}

    def __init__(
        self,
        action_id_reader: MinecraftConversationActionIdReader | None = None,
        status_action_reader: MinecraftConversationStatusActionReader | None = None,
        clock: Callable[[], float] | None = None,
        sleeper: Callable[[float], None] | None = None,
        max_wait_sec: float = 1800.0,
        poll_interval_sec: float = 2.0,
    ):
        self.action_id_reader = action_id_reader or MinecraftConversationActionIdReader()
        self.status_action_reader = (
            status_action_reader or MinecraftConversationStatusActionReader()
        )
        self.clock = clock or time.monotonic
        self.sleeper = sleeper or time.sleep
        self.max_wait_sec = max_wait_sec
        self.poll_interval_sec = poll_interval_sec

    def wait(self, extension: Any, initial_result: Mapping[str, Any]) -> dict[str, Any] | None:
        action_id = self.action_id_reader.read(initial_result)
        if not action_id:
            return None

        deadline = self.clock() + self.max_wait_sec
        last_action = self._initial_action(initial_result)

        while self.clock() < deadline:
            action = self._read_current_action(extension)
            if action is None:
                return self._result(
                    initial_result,
                    ok=False,
                    completion_status="current_action_unavailable",
                    action=last_action,
                    message="Minecraft current action became unavailable.",
                )

            last_action = action
            if str(action.get("action_id") or "") != action_id:
                return self._result(
                    initial_result,
                    ok=False,
                    completion_status="action_replaced",
                    action=action,
                    message="Minecraft action changed before the watcher finished.",
                )

            action_status = str(action.get("status") or "").strip().lower()
            if action_status in self.TERMINAL_STATUSES:
                return self._result(
                    initial_result,
                    ok=action_status == "succeeded",
                    completion_status=action_status,
                    action=action,
                )

            self.sleeper(self.poll_interval_sec)

        return self._result(
            initial_result,
            ok=False,
            completion_status="watch_timeout",
            action=last_action,
            message="Minecraft action is still running after the extended watch timeout.",
        )

    def _read_current_action(self, extension: Any) -> dict[str, Any] | None:
        get_status = getattr(extension, "get_status", None)
        if not callable(get_status):
            return None
        try:
            return self.status_action_reader.read(get_status())
        except Exception as error:
            log_print(f"[MinecraftConversation] current action watch failed: {error}")
            return None

    def _initial_action(self, result: Mapping[str, Any]) -> dict[str, Any] | None:
        action = result.get("action")
        if isinstance(action, Mapping):
            return dict(action)
        completion = result.get("completion")
        if isinstance(completion, Mapping) and isinstance(completion.get("action"), Mapping):
            return dict(completion["action"])
        return None

    def _result(
        self,
        initial_result: Mapping[str, Any],
        *,
        ok: bool,
        completion_status: str,
        action: Mapping[str, Any] | None,
        message: str = "",
    ) -> dict[str, Any]:
        result = dict(initial_result)
        result["ok"] = ok
        if not ok:
            result["error"] = "action_completion_failed"
        else:
            result.pop("error", None)
        if message:
            result["message"] = message

        completion = dict(result.get("completion") or {})
        completion["ok"] = ok
        completion["completion_status"] = completion_status
        completion["action_status"] = completion_status
        if action is not None:
            completion["action"] = dict(action)
            result["action"] = dict(action)
        result["completion"] = completion
        return result
