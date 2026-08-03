#20260803_kpopmodder: Added a command-free DTO for Korean ChatClef intents.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


@dataclass(frozen=True)
class ChatClefIntentDTO:
    intent_type: ChatClefIntentType = ChatClefIntentType.UNKNOWN
    quantity: int | None = None
    item_phrase: str = ""
    food_units: int | None = None
    x: int | None = None
    y: int | None = None
    z: int | None = None
    player_name: str = ""
    original_text: str = ""
    source: str = "rule"
    confidence: float = 1.0
    language: str = "ko"
    slots: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        object.__setattr__(self, "intent_type", ChatClefIntentType(self.intent_type))
        object.__setattr__(self, "item_phrase", str(self.item_phrase or ""))
        object.__setattr__(self, "player_name", str(self.player_name or ""))
        object.__setattr__(self, "original_text", str(self.original_text or ""))
        object.__setattr__(self, "source", str(self.source or ""))
        object.__setattr__(self, "language", str(self.language or "ko"))
        object.__setattr__(self, "confidence", float(self.confidence))
        object.__setattr__(self, "slots", dict(self.slots or {}))
        object.__setattr__(self, "quantity", self._optional_int(self.quantity))
        object.__setattr__(self, "food_units", self._optional_int(self.food_units))
        object.__setattr__(self, "x", self._optional_int(self.x))
        object.__setattr__(self, "y", self._optional_int(self.y))
        object.__setattr__(self, "z", self._optional_int(self.z))

    @classmethod
    def from_mapping(cls, value: Any) -> "ChatClefIntentDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            intent_type=payload.get("intent_type", ChatClefIntentType.UNKNOWN),
            quantity=payload.get("quantity"),
            item_phrase=payload.get("item_phrase", ""),
            food_units=payload.get("food_units"),
            x=payload.get("x"),
            y=payload.get("y"),
            z=payload.get("z"),
            player_name=payload.get("player_name", ""),
            original_text=payload.get("original_text", ""),
            source=payload.get("source", "rule"),
            confidence=payload.get("confidence", 1.0),
            language=payload.get("language", "ko"),
            slots=payload.get("slots", {}),
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "intent_type": self.intent_type.value,
            "quantity": self.quantity,
            "item_phrase": self.item_phrase,
            "food_units": self.food_units,
            "x": self.x,
            "y": self.y,
            "z": self.z,
            "player_name": self.player_name,
            "original_text": self.original_text,
            "source": self.source,
            "confidence": self.confidence,
            "language": self.language,
            "slots": dict(self.slots),
        }

    def _optional_int(self, value: Any) -> int | None:
        if value is None or value == "":
            return None
        return int(value)
