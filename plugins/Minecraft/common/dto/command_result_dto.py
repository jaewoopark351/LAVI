#20260801_kpopmodder: Define command result DTOs with explicit status and nullable errors.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Mapping, Optional

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


@dataclass(frozen=True)
class CommandResultDTO:
    request_id: str = ""
    ok: bool = False
    status: CommandResultStatus = CommandResultStatus.UNKNOWN
    error_code: Optional[BridgeErrorCode] = None
    message: str = ""
    data: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        status = CommandResultStatus(self.status)
        error_code = None if self.error_code is None else BridgeErrorCode(self.error_code)
        ok = bool(self.ok)
        if ok != status.ok:
            raise ValueError("ok must match CommandResultStatus.ok")
        if ok and error_code is not None:
            raise ValueError("successful command results must not include error_code")
        object.__setattr__(self, "request_id", str(self.request_id or ""))
        object.__setattr__(self, "ok", ok)
        object.__setattr__(self, "status", status)
        object.__setattr__(self, "error_code", error_code)
        object.__setattr__(self, "message", str(self.message or ""))
        object.__setattr__(self, "data", _coerce_dict(self.data))

    @classmethod
    def from_mapping(cls, value: Any) -> "CommandResultDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            request_id=payload.get("request_id", ""),
            ok=payload.get("ok", False),
            status=payload.get("status", CommandResultStatus.UNKNOWN),
            error_code=payload.get("error_code"),
            message=payload.get("message", ""),
            data=_coerce_dict(payload.get("data")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "request_id": self.request_id,
            "ok": self.ok,
            "status": self.status.value,
            "error_code": None if self.error_code is None else self.error_code.value,
            "message": self.message,
            "data": dict(self.data),
        }
