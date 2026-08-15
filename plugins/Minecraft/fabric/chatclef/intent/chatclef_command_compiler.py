#20260803_kpopmodder: Added strict ChatClef DSL compilation from validated intents.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_safety import (
    ChatClefCommandSafetyValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    ChatClefNumericConstraints,
)


class ChatClefCommandCompiler:
    _TARGET_RE = re.compile(r"^[a-z0-9_]+$")
    _PLAYER_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")
    _DANGEROUS_RE = re.compile(r"[;#\r\n\"'@]|[\x00-\x1f\x7f]")

    def compile(self, intent: ChatClefIntentDTO, target: str | None = None) -> str:
        self.reject_dangerous_text(intent.original_text)
        if intent.intent_type is ChatClefIntentType.GET_ITEM:
            target_text = self._target(target)
            quantity = self._positive_int(intent.quantity, "quantity")
            return f"get {target_text} {quantity}"
        if intent.intent_type is ChatClefIntentType.FOOD:
            return f"food {self._positive_int(intent.food_units, 'food_units')}"
        if intent.intent_type is ChatClefIntentType.MEAT:
            return f"meat {self._positive_int(intent.food_units, 'food_units')}"
        if intent.intent_type is ChatClefIntentType.GOTO:
            x = self._java_int(intent.x, "x")
            y = self._java_int(intent.y, "y")
            z = self._java_int(intent.z, "z")
            return f"goto {x} {y} {z}"
        if intent.intent_type is ChatClefIntentType.FOLLOW:
            player_name = intent.player_name
            self.reject_dangerous_text(player_name)
            if not self._PLAYER_RE.fullmatch(player_name):
                raise ValueError("invalid_player_name")
            return f"follow {player_name}"
        if intent.intent_type is ChatClefIntentType.IDLE:
            return "idle"
        if intent.intent_type is ChatClefIntentType.STOP:
            return "stop"
        raise ValueError("unsupported_intent_type")

    def has_dangerous_text(self, text: object) -> bool:
        return ChatClefCommandSafetyValidator.has_dangerous_text(text)

    def reject_dangerous_text(self, text: object) -> None:
        if self.has_dangerous_text(text):
            raise ValueError("dangerous_command_slot")

    def _target(self, target: str | None) -> str:
        target_text = str(target or "")
        self.reject_dangerous_text(target_text)
        if not self._TARGET_RE.fullmatch(target_text):
            raise ValueError("invalid_target")
        return target_text

    def _positive_int(self, value: int | None, field_name: str) -> int:
        return ChatClefNumericConstraints.positive_java_int(value, field_name)

    def _required_int(self, value: int | None, field_name: str) -> int:
        if value is None:
            raise ValueError(f"missing_{field_name}")
        return ChatClefNumericConstraints.required_exact_int(value, field_name)

    def _java_int(self, value: int | None, field_name: str) -> int:
        if value is None:
            raise ValueError(f"missing_{field_name}")
        return ChatClefNumericConstraints.java_int(value, field_name)
