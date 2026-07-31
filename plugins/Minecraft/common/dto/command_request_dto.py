#20260801_kpopmodder: Define Minecraft command requests without transport timeout ownership.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Mapping, Optional


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


@dataclass(frozen=True)
class CommandRequestDTO:
    request_id: str = ""
    command: str = ""
    source: str = ""
    deadline_ms: Optional[int] = None
    metadata: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        object.__setattr__(self, "request_id", str(self.request_id or ""))
        object.__setattr__(self, "command", str(self.command or ""))
        object.__setattr__(self, "source", str(self.source or ""))
        deadline = None if self.deadline_ms is None else int(self.deadline_ms)
        object.__setattr__(self, "deadline_ms", deadline)
        object.__setattr__(self, "metadata", _coerce_dict(self.metadata))

    @classmethod
    def from_mapping(cls, value: Any) -> "CommandRequestDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            request_id=payload.get("request_id", ""),
            command=payload.get("command", ""),
            source=payload.get("source", ""),
            deadline_ms=payload.get("deadline_ms"),
            metadata=_coerce_dict(payload.get("metadata")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "request_id": self.request_id,
            "command": self.command,
            "source": self.source,
            "deadline_ms": self.deadline_ms,
            "metadata": dict(self.metadata),
        }
