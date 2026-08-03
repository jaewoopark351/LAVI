#20260803_kpopmodder: Added executable translation result separate from intent data.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)


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
        executable = bool(self.executable)
        if executable != status.executable:
            raise ValueError("executable must match ChatClefIntentStatus")
        if not executable and self.command is not None:
            raise ValueError("non-executable translations must not include command")
        object.__setattr__(self, "status", status)
        object.__setattr__(self, "executable", executable)
        object.__setattr__(self, "command", None if self.command is None else str(self.command))
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
