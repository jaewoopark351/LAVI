#20260715_kpopmodder: Added typed contracts for game extension command/status/result handoffs.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping, Optional


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


def _coerce_action(value: Any) -> str:
    return str(value or "").strip().lower()


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
            details=_coerce_dict(payload.get("details")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class GameStartResultDTO(GameResultDTO):
    action: str = "start"
    started: bool = False


@dataclass(frozen=True)
class GameStopResultDTO(GameResultDTO):
    action: str = "stop"
    stopped: bool = False


@dataclass(frozen=True)
class GameStatusDTO:
    name: str = ""
    initialized: bool = False
    started: bool = False
    plugin: Dict[str, Any] = field(default_factory=dict)
    worker: Dict[str, Any] = field(default_factory=dict)
    runtime: Dict[str, Any] = field(default_factory=dict)
    details: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None

    @classmethod
    def from_mapping(cls, value: Any, name: str = "") -> "GameStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        extension = _coerce_dict(payload.get("extension"))
        return cls(
            name=str(name or extension.get("name") or payload.get("name") or "").strip(),
            initialized=bool(extension.get("initialized", payload.get("initialized", False))),
            started=bool(extension.get("started", payload.get("started", False))),
            plugin=_coerce_dict(payload.get("plugin")),
            worker=_coerce_dict(payload.get("worker")),
            runtime=_coerce_dict(payload.get("runtime_context") or payload.get("runtime")),
            details=_coerce_dict(payload.get("details")),
            error=None if payload.get("error") is None else str(payload.get("error")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

