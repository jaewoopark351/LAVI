#20260717_kpopmodder: Isolates the base game result DTO for extension responses.
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping, Optional

from .contract_helpers import (
    _RESULT_RESERVED_KEYS,
    _coerce_action,
    _coerce_dict,
    _merge_details,
)


@dataclass(frozen=True)
class GameResultDTO:
    ok: bool = False
    action: str = ""
    status: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    message: Optional[str] = None
    details: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any, action: str = "") -> "GameResultDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            ok=bool(payload.get("ok", False)),
            action=_coerce_action(action or payload.get("action")),
            status=_coerce_dict(payload.get("status")),
            error=None if payload.get("error") is None else str(payload.get("error")),
            message=None if payload.get("message") is None else str(payload.get("message")),
            details=_merge_details(payload, _RESULT_RESERVED_KEYS),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = dict(self.details)
        payload.update(
            {
                "ok": self.ok,
                "action": self.action,
                "status": dict(self.status),
                "error": self.error,
                "message": self.message,
            }
        )
        if not self.status:
            payload.pop("status", None)
        if self.error is None:
            payload.pop("error", None)
        if self.message is None:
            payload.pop("message", None)
        if self.details:
            payload["details"] = dict(self.details)
        return payload
