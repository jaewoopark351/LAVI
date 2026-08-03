#20260803_kpopmodder: Added strict intent validation before DSL compilation.
from __future__ import annotations

import re
from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class ChatClefIntentSchemaValidator:
    _ALLOWED_FIELDS = set(ChatClefIntentDTO().to_dict())
    _FORBIDDEN_FIELDS = {
        "command",
        "chatclef_command",
        "target",
        "minecraft_id",
        "dsl",
        "executable",
    }
    _PLAYER_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")

    def validate(self, value: Any) -> tuple[bool, str, str]:
        try:
            payload = self._payload(value)
            forbidden = sorted(set(payload) & self._FORBIDDEN_FIELDS)
            if forbidden:
                return False, "forbidden_intent_fields", ",".join(forbidden)
            extra = sorted(set(payload) - self._ALLOWED_FIELDS)
            if extra:
                return False, "unknown_intent_fields", ",".join(extra)
            intent = ChatClefIntentDTO.from_mapping(payload)
        except Exception as error:
            return False, "malformed_intent", f"{type(error).__name__}: {error}"
        return self._validate_intent(intent)

    def _payload(self, value: Any) -> Mapping[str, Any]:
        if isinstance(value, ChatClefIntentDTO):
            return value.to_dict()
        if isinstance(value, Mapping):
            return value
        raise TypeError("intent must be a mapping or ChatClefIntentDTO")

    def _validate_intent(self, intent: ChatClefIntentDTO) -> tuple[bool, str, str]:
        if intent.intent_type is ChatClefIntentType.UNKNOWN:
            return True, "validated_unknown_intent", "unknown intent is non-executable"
        if intent.intent_type is ChatClefIntentType.GET_ITEM:
            if not intent.item_phrase.strip():
                return False, "missing_item_phrase", "get_item requires item_phrase"
            if intent.quantity is None or intent.quantity <= 0:
                return False, "invalid_quantity", "get_item quantity must be positive"
        if intent.intent_type in {ChatClefIntentType.FOOD, ChatClefIntentType.MEAT}:
            if intent.food_units is None or intent.food_units <= 0:
                return False, "invalid_food_units", "food units must be positive"
        if intent.intent_type is ChatClefIntentType.GOTO:
            if intent.x is None or intent.y is None or intent.z is None:
                return False, "missing_coordinates", "goto requires x, y, and z"
        if intent.intent_type is ChatClefIntentType.FOLLOW:
            if not self._PLAYER_RE.fullmatch(intent.player_name):
                return False, "invalid_player_name", "follow requires a valid player name"
        return True, "validated_intent", "intent schema is valid"
