#20260803_kpopmodder: Added executable translation result separate from intent data.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_validator import (
    ChatClefTranslationResultValidator,
)


def _coerce_dict(value: Any) -> dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


@dataclass(frozen=True)
class ChatClefTranslationResultDTO:
    status: ChatClefIntentStatus
    executable: bool
    command: str | None = None
    intent: ChatClefIntentDTO | None = None
    resolved_target: str | None = None
    reason_code: str = ""
    message: str = ""
    data: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        status = ChatClefIntentStatus(self.status)
        intent = self.intent
        if intent is not None and not isinstance(intent, ChatClefIntentDTO):
            intent = ChatClefIntentDTO.from_mapping(intent)
        command = ChatClefTranslationResultValidator().validate(
            status=status,
            executable=self.executable,
            command=self.command,
            intent=intent,
            resolved_target=self.resolved_target,
        )
        object.__setattr__(self, "status", status)
        object.__setattr__(self, "executable", self.executable)
        object.__setattr__(self, "command", command)
        object.__setattr__(self, "intent", intent)
        object.__setattr__(
            self,
            "resolved_target",
            None if self.resolved_target is None else str(self.resolved_target),
        )
        object.__setattr__(self, "reason_code", str(self.reason_code or ""))
        object.__setattr__(self, "message", str(self.message or ""))
        object.__setattr__(self, "data", dict(self.data or {}))

    @classmethod
    def validated(
        cls,
        command: str,
        intent: ChatClefIntentDTO,
        resolved_target: str | None = None,
        data: dict[str, Any] | None = None,
    ) -> "ChatClefTranslationResultDTO":
        return cls(
            status=ChatClefIntentStatus.VALIDATED,
            executable=True,
            command=command,
            intent=intent,
            resolved_target=resolved_target,
            reason_code="validated",
            message="Korean command was translated to ChatClef DSL.",
            data=data or {},
        )

    @classmethod
    def rejected(
        cls,
        status: ChatClefIntentStatus,
        reason_code: str,
        message: str,
        intent: ChatClefIntentDTO | None = None,
        data: dict[str, Any] | None = None,
    ) -> "ChatClefTranslationResultDTO":
        return cls(
            status=status,
            executable=False,
            command=None,
            intent=intent,
            resolved_target=None,
            reason_code=reason_code,
            message=message,
            data=data or {},
        )

    @classmethod
    def from_mapping(cls, value: Any) -> "ChatClefTranslationResultDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        intent_payload = payload.get("intent")
        intent = (
            None
            if intent_payload is None
            else ChatClefIntentDTO.from_mapping(intent_payload)
        )
        return cls(
            status=payload.get("status", ChatClefIntentStatus.UNKNOWN),
            executable=payload.get("executable", False),
            command=payload.get("command"),
            intent=intent,
            resolved_target=payload.get("resolved_target"),
            reason_code=payload.get("reason_code", ""),
            message=payload.get("message", ""),
            data=_coerce_dict(payload.get("data")),
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "status": self.status.value,
            "executable": self.executable,
            "command": self.command,
            "intent": None if self.intent is None else self.intent.to_dict(),
            "resolved_target": self.resolved_target,
            "reason_code": self.reason_code,
            "message": self.message,
            "data": dict(self.data),
        }
