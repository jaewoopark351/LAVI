#20260801_kpopmodder: Keep bridge envelope identity separate from command request identity.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Mapping, Optional

from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


@dataclass(frozen=True)
class BridgeEnvelopeDTO:
    protocol_version: int = 1
    message_type: BridgeMessageType = BridgeMessageType.ERROR
    message_id: str = ""
    correlation_id: Optional[str] = None
    session_id: Optional[str] = None
    timestamp_ms: int = 0
    payload: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        object.__setattr__(self, "protocol_version", int(self.protocol_version))
        object.__setattr__(self, "message_type", BridgeMessageType(self.message_type))
        object.__setattr__(self, "message_id", str(self.message_id or ""))
        object.__setattr__(
            self,
            "correlation_id",
            None if self.correlation_id is None else str(self.correlation_id),
        )
        object.__setattr__(
            self,
            "session_id",
            None if self.session_id is None else str(self.session_id),
        )
        object.__setattr__(self, "timestamp_ms", int(self.timestamp_ms or 0))
        object.__setattr__(self, "payload", _coerce_dict(self.payload))

    @classmethod
    def from_mapping(cls, value: Any) -> "BridgeEnvelopeDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            protocol_version=payload.get("protocol_version", 1),
            message_type=payload.get("message_type", BridgeMessageType.ERROR),
            message_id=payload.get("message_id", ""),
            correlation_id=payload.get("correlation_id"),
            session_id=payload.get("session_id"),
            timestamp_ms=payload.get("timestamp_ms", 0),
            payload=_coerce_dict(payload.get("payload")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "protocol_version": self.protocol_version,
            "message_type": self.message_type.value,
            "message_id": self.message_id,
            "correlation_id": self.correlation_id,
            "session_id": self.session_id,
            "timestamp_ms": self.timestamp_ms,
            "payload": dict(self.payload),
        }
