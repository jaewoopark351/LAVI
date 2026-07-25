#20260725_kpopmodder: Added plugin invoker so dispatcher does not own plugin call details.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_command_dispatch_result import MinecraftCommandDispatchResult


class MinecraftPluginCommandInvoker:
    def invoke(
        self,
        plugin: Any,
        command: Any,
        payload: Dict[str, Any],
        action: str,
        *,
        is_text_command: bool,
    ) -> MinecraftCommandDispatchResult:
        handler = getattr(plugin, "handle_command", None)
        if not callable(handler):
            return MinecraftCommandDispatchResult(
                {"ok": False, "action": action, "error": "missing_plugin_handler"},
                action,
            )

        if is_text_command:
            return MinecraftCommandDispatchResult(handler(command), action)

        payload["action"] = action
        return MinecraftCommandDispatchResult(handler(payload), action)
