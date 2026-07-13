#20260628_kpopmodder: Add shared StarCraft2 contracts to replace ad-hoc dict payloads.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Optional

import time


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, dict) else {}


def _normalize_ports(value: Any) -> list[int]:
    if value is None:
        return [5677, 5678]
    if isinstance(value, str):
        raw = [part.strip() for part in value.split(",")]
    elif isinstance(value, (list, tuple)):
        raw = [str(part).strip() for part in value]
    else:
        return [5677, 5678]
    ports: list[int] = []
    for item in raw:
        try:
            port = int(item)
        except (TypeError, ValueError):
            continue
        if 0 < port < 65536 and port not in ports:
            ports.append(port)
    return ports or [5677, 5678]


def _coerce_str(value: Any, fallback: str = "") -> str:
    return str(value or "").strip() if value is not None else fallback


@dataclass(frozen=True)
class LocalMatchLaunchConfigDTO:
    executable_path: str = ""
    working_directory: str = ""
    args: str = ""
    proxy_ports: list[int] = field(default_factory=lambda: [5677, 5678])
    bot_name: str = ""
    ai_race: str = "Zerg"
    human_race: str = "Terran"
    capture_output: bool = True
    keep_local_match_identity_args: bool = False
    starcraft2_exe_path: str = ""
    starcraft2_support64_path: str = ""
    starcraft2_base_path: str = ""
    check_hosts: list[str] = field(default_factory=lambda: ["127.0.0.1"])

    @classmethod
    def from_mapping(cls, value: Any) -> "LocalMatchLaunchConfigDTO":
        payload = value if isinstance(value, dict) else {}
        args = payload.get("args", "")
        if isinstance(args, list):
            args_text = " ".join(str(item) for item in args)
        else:
            args_text = _coerce_str(args, "")
        return cls(
            executable_path=_coerce_str(payload.get("executable_path")),
            working_directory=_coerce_str(payload.get("working_directory")),
            args=args_text,
            proxy_ports=_normalize_ports(payload.get("ports")),
            bot_name=_coerce_str(payload.get("bot_name")),
            ai_race=_coerce_str(payload.get("ai_race"), "Zerg"),
            human_race=_coerce_str(payload.get("human_race"), "Terran"),
            capture_output=(
                bool(payload.get("capture_output"))
                if isinstance(payload, dict) and "capture_output" in payload
                else True
            ),
            keep_local_match_identity_args=bool(payload.get("keep_local_match_identity_args")),
            starcraft2_exe_path=_coerce_str(payload.get("starcraft2_exe_path")),
            starcraft2_support64_path=_coerce_str(payload.get("starcraft2_support64_path")),
            starcraft2_base_path=_coerce_str(payload.get("starcraft2_base_path")),
            check_hosts=[_coerce_str(item) for item in payload.get("check_hosts", ["127.0.0.1"]) if _coerce_str(item)],
        )

    def with_args(self, args: str) -> "LocalMatchLaunchConfigDTO":
        return LocalMatchLaunchConfigDTO(
            executable_path=self.executable_path,
            working_directory=self.working_directory,
            args=_coerce_str(args),
            proxy_ports=list(self.proxy_ports),
            bot_name=self.bot_name,
            ai_race=self.ai_race,
            human_race=self.human_race,
            capture_output=self.capture_output,
            keep_local_match_identity_args=self.keep_local_match_identity_args,
            starcraft2_exe_path=self.starcraft2_exe_path,
            starcraft2_support64_path=self.starcraft2_support64_path,
            starcraft2_base_path=self.starcraft2_base_path,
            check_hosts=list(self.check_hosts),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "executable_path": self.executable_path,
            "working_directory": self.working_directory,
            "args": self.args,
            "ports": list(self.proxy_ports),
            "check_hosts": list(self.check_hosts),
            "bot_name": self.bot_name,
            "ai_race": self.ai_race,
            "human_race": self.human_race,
            "capture_output": bool(self.capture_output),
            "keep_local_match_identity_args": bool(self.keep_local_match_identity_args),
            "starcraft2_exe_path": self.starcraft2_exe_path,
            "starcraft2_support64_path": self.starcraft2_support64_path,
            "starcraft2_base_path": self.starcraft2_base_path,
        }


@dataclass(frozen=True)
class StartResultDTO:
    ok: bool
    running: bool = False
    action: str = "local_match"
    status: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    message: Optional[str] = None
    details: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any, action: str = "local_match") -> "StartResultDTO":
        payload = value if isinstance(value, dict) else {}
        return cls(
            ok=bool(payload.get("ok", False)),
            running=bool(payload.get("running", False)),
            action=action if action else str(payload.get("action") or "local_match"),
            status=_coerce_dict(payload.get("status")),
            error=None if payload.get("error") is None else str(payload.get("error")),
            message=None if payload.get("message") is None else str(payload.get("message")),
            details=_coerce_dict(payload.get("details")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class StopResultDTO(StartResultDTO):
    action: str = "stop"
    stopped: bool = False

    @classmethod
    def from_mapping(cls, value: Any, action: str = "stop") -> "StopResultDTO":
        payload = value if isinstance(value, dict) else {}
        return cls(
            ok=bool(payload.get("ok", False)),
            running=bool(payload.get("running", False)),
            action=action if action else str(payload.get("action") or "stop"),
            status=_coerce_dict(payload.get("status")),
            error=None if payload.get("error") is None else str(payload.get("error")),
            message=None if payload.get("message") is None else str(payload.get("message")),
            details=_coerce_dict(payload.get("details")),
            stopped=bool(payload.get("stopped", False)),
        )


@dataclass(frozen=True)
class LocalMatchRuntimeStatusDTO:
    mode: str = "local_human_vs_changeling"
    result: Optional[StartResultDTO] = None
    ladder_proxy: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "mode": str(self.mode),
            "result": (
                self.result.to_dict() if isinstance(self.result, StartResultDTO) else _coerce_dict(self.result)
            ),
            "ladder_proxy": _coerce_dict(self.ladder_proxy),
        }


@dataclass(frozen=True)
class StarCraft2Event:
    event_type: str
    details: Dict[str, Any] = field(default_factory=dict)
    source: str = "starcraft2"
    engine: str = "starcraft2"
    time: float = field(default_factory=lambda: round(time.time(), 6))

    @classmethod
    def from_mapping(cls, value: Any) -> "StarCraft2Event":
        payload = value if isinstance(value, dict) else {}
        return cls(
            event_type=str(payload.get("event_type") or ""),
            details=_coerce_dict(payload.get("details")),
            source=str(payload.get("source") or "starcraft2"),
            engine=str(payload.get("engine") or "starcraft2"),
            time=float(payload.get("time") or 0.0)
            if isinstance(payload.get("time"), (int, float))
            else time.time(),
        )

    def to_dict(self) -> Dict[str, Any]:
        payload = asdict(self)
        payload["details"] = _coerce_dict(self.details)
        return payload
