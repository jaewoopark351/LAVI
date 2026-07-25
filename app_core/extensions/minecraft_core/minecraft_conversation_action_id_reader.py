#20260725_kpopmodder: Added reader for bridge action ids in Minecraft conversation results.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftConversationActionIdReader:
    def read(self, result: Any) -> str:
        if not isinstance(result, Mapping):
            return ""

        direct = self._from_action(result.get("action"))
        if direct:
            return direct

        completion = result.get("completion")
        if isinstance(completion, Mapping):
            return self._from_action(completion.get("action"))

        details = result.get("details")
        if isinstance(details, Mapping):
            detail_completion = details.get("completion")
            if isinstance(detail_completion, Mapping):
                return self._from_action(detail_completion.get("action"))
        return ""

    def _from_action(self, action: Any) -> str:
        if not isinstance(action, Mapping):
            return ""
        return str(action.get("action_id") or "").strip()
