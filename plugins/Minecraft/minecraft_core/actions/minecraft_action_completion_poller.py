#20260725_kpopmodder: Added this poller to wait for bridge action completion without mixing inventory checks.
from __future__ import annotations

import time
from typing import Any, Callable, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider


class MinecraftActionCompletionPoller:
    TERMINAL_STATUSES = {"succeeded", "failed", "cancelled"}

    def __init__(
        self,
        client_provider: MinecraftBridgeClientProvider,
        clock: Callable[[], float] | None = None,
        sleeper: Callable[[float], None] | None = None,
    ):
        self.client_provider = client_provider
        self.clock = clock or time.monotonic
        self.sleeper = sleeper or time.sleep

    def wait(
        self,
        action_id: str,
        *,
        timeout_sec: float,
        poll_interval_sec: float,
    ) -> Dict[str, Any]:
        deadline = self.clock() + timeout_sec
        last_action: Dict[str, Any] | None = None

        while True:
            try:
                current = self.client_provider.client.current_action()
            except Exception as error:
                return {
                    "ok": False,
                    "completion_status": "current_action_unavailable",
                    "message": str(error),
                    "action": last_action,
                }
            if not isinstance(current, dict) or not current.get("ok"):
                return {
                    "ok": False,
                    "completion_status": "current_action_unavailable",
                    "message": "Minecraft current action is unavailable.",
                    "response": current,
                    "action": last_action,
                }

            action = current.get("action")
            if not isinstance(action, dict):
                return {
                    "ok": False,
                    "completion_status": "action_missing",
                    "message": "Minecraft bridge did not report a current action.",
                    "response": current,
                    "action": last_action,
                }

            last_action = action
            if action.get("action_id") != action_id:
                return {
                    "ok": False,
                    "completion_status": "action_replaced",
                    "message": "Minecraft current action changed before verification finished.",
                    "action": action,
                }

            action_status = str(action.get("status") or "").strip().lower()
            if action_status in self.TERMINAL_STATUSES:
                return {
                    "ok": action_status == "succeeded",
                    "completion_status": action_status,
                    "action_status": action_status,
                    "action": action,
                }

            if self.clock() >= deadline:
                return {
                    "ok": False,
                    "completion_status": "timeout",
                    "action_status": action_status,
                    "message": "Minecraft action verification timed out.",
                    "action": action,
                }

            self.sleeper(poll_interval_sec)
