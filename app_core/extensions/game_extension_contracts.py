#20260715_kpopmodder: Added typed contracts for game extension command/status/result handoffs.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping, Optional


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


def _coerce_action(value: Any) -> str:
    return str(value or "").strip().lower()


_RESULT_RESERVED_KEYS = {
    "ok",
    "action",
    "status",
    "error",
    "message",
    "details",
    "running",
    "started",
    "stopped",
}

_STATUS_RESERVED_KEYS = {
    "name",
    "initialized",
    "started",
    "plugin",
    "worker",
    "runtime",
    "runtime_context",
    "details",
    "error",
    "extension",
    "game_status",
}


def _merge_details(payload: Mapping[str, Any], reserved_keys: set[str]) -> Dict[str, Any]:
    details = _coerce_dict(payload.get("details"))
    for key, value in payload.items():
        if key in reserved_keys:
            continue
        details.setdefault(str(key), value)
    return details


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


@dataclass(frozen=True)
class GameStartResultDTO(GameResultDTO):
    action: str = "start"
    running: bool = False
    started: bool = False

    @classmethod
    def from_mapping(cls, value: Any, action: str = "start") -> "GameStartResultDTO":
        if isinstance(value, cls):
            return value
        base = GameResultDTO.from_mapping(value, action=action)
        payload = value if isinstance(value, Mapping) else {}
        running = bool(payload.get("running", False))
        return cls(
            ok=base.ok,
            action=base.action or "start",
            status=dict(base.status),
            error=base.error,
            message=base.message,
            details=dict(base.details),
            running=running,
            started=bool(payload.get("started", running)),
        )

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = super().to_legacy_dict()
        payload["running"] = bool(self.running)
        payload["started"] = bool(self.started)
        return payload


@dataclass(frozen=True)
class GameStopResultDTO(GameResultDTO):
    action: str = "stop"
    running: bool = False
    stopped: bool = False

    @classmethod
    def from_mapping(cls, value: Any, action: str = "stop") -> "GameStopResultDTO":
        if isinstance(value, cls):
            return value
        base = GameResultDTO.from_mapping(value, action=action)
        payload = value if isinstance(value, Mapping) else {}
        running = bool(payload.get("running", False))
        return cls(
            ok=base.ok,
            action=base.action or "stop",
            status=dict(base.status),
            error=base.error,
            message=base.message,
            details=dict(base.details),
            running=running,
            stopped=bool(payload.get("stopped", base.ok and not running)),
        )

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = super().to_legacy_dict()
        payload["running"] = bool(self.running)
        payload["stopped"] = bool(self.stopped)
        return payload


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
            details=_merge_details(payload, _STATUS_RESERVED_KEYS),
            error=None if payload.get("error") is None else str(payload.get("error")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = dict(self.details)
        payload.update(
            {
                "name": self.name,
                "initialized": self.initialized,
                "started": self.started,
                "plugin": dict(self.plugin),
                "worker": dict(self.worker),
                "runtime": dict(self.runtime),
                "error": self.error,
            }
        )
        if not self.plugin:
            payload.pop("plugin", None)
        if not self.worker:
            payload.pop("worker", None)
        if not self.runtime:
            payload.pop("runtime", None)
        if self.error is None:
            payload.pop("error", None)
        if self.details:
            payload["details"] = dict(self.details)
        return payload
