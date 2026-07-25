#20260725_kpopmodder: Added focused reader for Minecraft action metadata in conversation replies.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationActionMetadataReader:
    def action_type(
        self,
        route: MinecraftConversationCommandRoute,
        result: Mapping[str, Any],
    ) -> str:
        action_payload = self._action_payload(result)
        if isinstance(action_payload, Mapping):
            action = action_payload.get("type")
        else:
            action = action_payload

        action_text = str(action or "").strip().lower().replace("-", "_")
        if action_text:
            return action_text
        return self._route_action(route.command)

    def request(
        self,
        route: MinecraftConversationCommandRoute,
        result: Mapping[str, Any],
    ) -> dict[str, Any]:
        action_payload = self._action_payload(result)
        if isinstance(action_payload, Mapping) and isinstance(
            action_payload.get("request"),
            Mapping,
        ):
            return dict(action_payload["request"])
        if isinstance(result.get("request"), Mapping):
            return dict(result["request"])

        details = result.get("details")
        if isinstance(details, Mapping) and isinstance(details.get("request"), Mapping):
            return dict(details["request"])
        return self._route_request(route.command)

    def _action_payload(self, result: Mapping[str, Any]) -> Any:
        direct_action = result.get("action")
        if isinstance(direct_action, Mapping):
            return direct_action

        completion_action = self._completion_action(result)
        if isinstance(completion_action, Mapping):
            return completion_action

        return direct_action

    def _completion_action(self, result: Mapping[str, Any]) -> Any:
        completion = result.get("completion")
        if isinstance(completion, Mapping) and isinstance(completion.get("action"), Mapping):
            return completion["action"]

        details = result.get("details")
        if not isinstance(details, Mapping):
            return None
        detail_completion = details.get("completion")
        if isinstance(detail_completion, Mapping):
            return detail_completion.get("action")
        return None

    def _route_action(self, command: str) -> str:
        lowered = str(command or "").strip().lower()
        if lowered.startswith(("get and equip ", "get_and_equip ")):
            return "get_and_equip"
        if lowered.startswith("get "):
            return "get_item"
        if lowered.startswith("craft "):
            return "craft"
        if lowered.startswith("equip "):
            return "equip"
        if lowered.startswith(("goto ", "go to ")):
            return "goto"
        return lowered if lowered in {"stop", "inventory", "health", "status"} else ""

    def _route_request(self, command: str) -> dict[str, Any]:
        words = str(command or "").strip().split()
        if not words:
            return {}

        lowered = [word.lower() for word in words]
        if lowered[:3] == ["get", "and", "equip"] and len(words) >= 4:
            return self._item_request(words[3:])
        if lowered[0] in {"get", "craft", "equip"} and len(words) >= 2:
            return self._item_request(words[1:])
        if lowered[:2] == ["go", "to"]:
            return {"target": " ".join(words[2:])}
        if lowered[0] == "goto":
            return {"target": " ".join(words[1:])}
        return {}

    def _item_request(self, words: list[str]) -> dict[str, Any]:
        if not words:
            return {}
        count = self._count(words[-1])
        item_words = words[:-1] if count is not None else words
        request: dict[str, Any] = {"item": "_".join(item_words)}
        if count is not None:
            request["count"] = count
        return request

    def _count(self, value: Any) -> int | None:
        try:
            count = int(value)
        except (TypeError, ValueError):
            return None
        return count if count > 0 else None
