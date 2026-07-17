#20260717_kpopmodder: Isolates GameCommandDTO from other game extension DTOs.
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping

from .contract_helpers import _coerce_action, _coerce_dict


@dataclass(frozen=True)
class GameCommandDTO:
    action: str = ""
    source: str = ""
    target: str = ""
    payload: Dict[str, Any] = field(default_factory=dict)
    metadata: Dict[str, Any] = field(default_factory=dict)
    raw: Any = None

    @classmethod
    def from_mapping(cls, value: Any) -> "GameCommandDTO":
        if isinstance(value, cls):
            return value
        if isinstance(value, Mapping):
            payload = dict(value)
            action = _coerce_action(
                payload.get("action")
                or payload.get("type")
                or payload.get("event")
                or payload.get("event_type")
            )
            return cls(
                action=action,
                source=str(payload.get("source") or "").strip(),
                target=str(payload.get("target") or payload.get("game") or "").strip(),
                payload=payload,
                metadata=_coerce_dict(payload.get("metadata")),
                raw=value,
            )
        if isinstance(value, str):
            return cls(action=_coerce_action(value), raw=value)
        return cls(raw=value)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = dict(self.payload)
        payload.setdefault("action", self.action)
        if self.source:
            payload.setdefault("source", self.source)
        if self.target:
            payload.setdefault("target", self.target)
        if self.metadata:
            payload.setdefault("metadata", dict(self.metadata))
        return payload
