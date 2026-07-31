#20260801_kpopmodder: Define backend-neutral status snapshots with fail-closed lifecycle state.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Mapping, Optional

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import BridgeLifecycleState


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


@dataclass(frozen=True)
class StatusSnapshotDTO:
    backend_id: str = ""
    enabled: bool = False
    connected: bool = False
    lifecycle_state: BridgeLifecycleState = BridgeLifecycleState.NOT_IMPLEMENTED
    detail: str = ""
    details: Dict[str, Any] = field(default_factory=dict)
    last_error_code: Optional[BridgeErrorCode] = None
    last_error_message: Optional[str] = None

    def __post_init__(self) -> None:
        error_code = (
            None
            if self.last_error_code is None
            else BridgeErrorCode(self.last_error_code)
        )
        object.__setattr__(self, "backend_id", str(self.backend_id or ""))
        object.__setattr__(self, "enabled", bool(self.enabled))
        object.__setattr__(self, "connected", bool(self.connected))
        object.__setattr__(
            self,
            "lifecycle_state",
            BridgeLifecycleState(self.lifecycle_state),
        )
        object.__setattr__(self, "detail", str(self.detail or ""))
        object.__setattr__(self, "details", _coerce_dict(self.details))
        object.__setattr__(self, "last_error_code", error_code)
        object.__setattr__(
            self,
            "last_error_message",
            None if self.last_error_message is None else str(self.last_error_message),
        )

    @classmethod
    def from_mapping(cls, value: Any) -> "StatusSnapshotDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            backend_id=payload.get("backend_id", ""),
            enabled=payload.get("enabled", False),
            connected=payload.get("connected", False),
            lifecycle_state=payload.get(
                "lifecycle_state",
                BridgeLifecycleState.NOT_IMPLEMENTED,
            ),
            detail=payload.get("detail", ""),
            details=_coerce_dict(payload.get("details")),
            last_error_code=payload.get("last_error_code"),
            last_error_message=payload.get("last_error_message"),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "backend_id": self.backend_id,
            "enabled": self.enabled,
            "connected": self.connected,
            "lifecycle_state": self.lifecycle_state.value,
            "detail": self.detail,
            "details": dict(self.details),
            "last_error_code": (
                None if self.last_error_code is None else self.last_error_code.value
            ),
            "last_error_message": self.last_error_message,
        }
