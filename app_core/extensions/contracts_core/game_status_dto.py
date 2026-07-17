#20260717_kpopmodder: Isolates game extension status DTO behavior.
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping, Optional

from .contract_helpers import (
    _STATUS_RESERVED_KEYS,
    _coerce_dict,
    _merge_details,
)


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
