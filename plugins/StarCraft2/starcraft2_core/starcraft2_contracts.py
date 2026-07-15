#20260628_kpopmodder: Add shared StarCraft2 contracts to replace ad-hoc dict payloads.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Optional

import time

from app_core.extensions.game_extension_contracts import GameResultDTO, GameStatusDTO


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


def _coerce_float(value: Any, fallback: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(fallback)


def _coerce_int(value: Any, fallback: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return int(fallback)


def _coerce_optional_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


#20260715_kpopmodder: Add typed command, result, and status contracts for SC2 engines.
@dataclass(frozen=True)
class EngineStartCommandDTO:
    """Stable command passed from the SC2 orchestrator to an engine."""

    config: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "EngineStartCommandDTO":
        if isinstance(value, cls):
            return value
        return cls(config=_coerce_dict(value))

    def to_dict(self) -> Dict[str, Any]:
        return dict(self.config)


@dataclass(frozen=True)
class EngineStatusDTO:
    """Stable status returned by every SC2 engine contract."""

    engine: str = "unknown"
    running: bool = False
    status: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None

    @classmethod
    def from_mapping(
        cls,
        value: Any,
        engine: str = "unknown",
    ) -> "EngineStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        normalized_engine = _coerce_str(payload.get("engine"), engine or "unknown")
        nested_status = payload.get("status")
        status = _coerce_dict(nested_status) if isinstance(nested_status, dict) else dict(payload)
        status.pop("engine", None)
        status.pop("error", None)
        running = bool(payload.get("running", status.get("running", False)))
        status["running"] = running
        error = payload.get("error", status.get("last_error"))
        return cls(
            engine=normalized_engine,
            running=running,
            status=status,
            error=None if error is None else str(error),
        )

    def to_dict(self) -> Dict[str, Any]:
        payload = dict(self.status)
        payload["engine"] = self.engine
        payload["running"] = bool(self.running)
        if self.error is not None:
            payload["error"] = self.error
        return payload


@dataclass(frozen=True)
class EngineResultDTO:
    """Stable start/stop result returned by an SC2 engine."""

    ok: bool
    engine: str = "unknown"
    running: bool = False
    status: EngineStatusDTO = field(default_factory=EngineStatusDTO)
    error: Optional[str] = None
    stopped: bool = False
    details: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(
        cls,
        value: Any,
        engine: str = "unknown",
    ) -> "EngineResultDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        normalized_engine = _coerce_str(payload.get("engine"), engine or "unknown")
        status = EngineStatusDTO.from_mapping(
            payload.get("status"),
            engine=normalized_engine,
        )
        running = bool(payload.get("running", status.running))
        if running != status.running:
            status = EngineStatusDTO(
                engine=status.engine,
                running=running,
                status={**status.status, "running": running},
                error=status.error,
            )
        return cls(
            ok=bool(payload.get("ok", False)),
            engine=normalized_engine,
            running=running,
            status=status,
            error=None if payload.get("error") is None else str(payload.get("error")),
            stopped=bool(payload.get("stopped", False)),
            details=_coerce_dict(payload.get("details")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ok": bool(self.ok),
            "engine": self.engine,
            "running": bool(self.running),
            "status": self.status.to_dict(),
            "error": self.error,
            "stopped": bool(self.stopped),
            "details": dict(self.details),
        }


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
    proxy_host: str = ""
    connect_timeout_sec: float = 0.5
    restart_unhealthy: bool = True
    restart_unhealthy_after_sec: float = 20.0
    runtime_download: Dict[str, Any] = field(default_factory=dict)
    bot_profile_validation: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LocalMatchLaunchConfigDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        args = payload.get("args", "")
        if isinstance(args, list):
            args_text = " ".join(str(item) for item in args)
        else:
            args_text = _coerce_str(args, "")
        raw_hosts = payload.get("check_hosts", ["127.0.0.1"])
        if isinstance(raw_hosts, str):
            normalized_hosts = [
                part.strip() for part in raw_hosts.split(",") if part.strip()
            ]
        elif isinstance(raw_hosts, (list, tuple)):
            normalized_hosts = [
                _coerce_str(item) for item in raw_hosts if _coerce_str(item)
            ]
        else:
            normalized_hosts = []
        return cls(
            executable_path=_coerce_str(payload.get("executable_path")),
            working_directory=_coerce_str(payload.get("working_directory")),
            args=args_text,
            proxy_ports=_normalize_ports(
                payload.get("proxy_ports", payload.get("ports"))
            ),
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
            check_hosts=normalized_hosts or ["127.0.0.1"],
            proxy_host=_coerce_str(payload.get("proxy_host")),
            connect_timeout_sec=_coerce_float(payload.get("connect_timeout_sec"), 0.5),
            restart_unhealthy=(
                bool(payload.get("restart_unhealthy"))
                if "restart_unhealthy" in payload
                else True
            ),
            restart_unhealthy_after_sec=_coerce_float(
                payload.get("restart_unhealthy_after_sec"),
                20.0,
            ),
            runtime_download=_coerce_dict(payload.get("runtime_download")),
            bot_profile_validation=_coerce_dict(
                payload.get("bot_profile_validation")
            ),
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
            proxy_host=self.proxy_host,
            connect_timeout_sec=self.connect_timeout_sec,
            restart_unhealthy=self.restart_unhealthy,
            restart_unhealthy_after_sec=self.restart_unhealthy_after_sec,
            runtime_download=dict(self.runtime_download),
            bot_profile_validation=dict(self.bot_profile_validation),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "executable_path": self.executable_path,
            "working_directory": self.working_directory,
            "args": self.args,
            "ports": list(self.proxy_ports),
            "check_hosts": list(self.check_hosts),
            "proxy_host": self.proxy_host,
            "connect_timeout_sec": self.connect_timeout_sec,
            "restart_unhealthy": bool(self.restart_unhealthy),
            "restart_unhealthy_after_sec": self.restart_unhealthy_after_sec,
            "runtime_download": dict(self.runtime_download),
            "bot_profile_validation": dict(self.bot_profile_validation),
            "bot_name": self.bot_name,
            "ai_race": self.ai_race,
            "human_race": self.human_race,
            "capture_output": bool(self.capture_output),
            "keep_local_match_identity_args": bool(self.keep_local_match_identity_args),
            "starcraft2_exe_path": self.starcraft2_exe_path,
            "starcraft2_support64_path": self.starcraft2_support64_path,
            "starcraft2_base_path": self.starcraft2_base_path,
        }


#20260715_kpopmodder: Type the Local Match ladder-proxy process boundary.
@dataclass(frozen=True)
class LadderProxyPortCheckDTO:
    host: str
    port: int
    open: bool = False
    error: str = ""

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyPortCheckDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        return cls(
            host=_coerce_str(payload.get("host")),
            port=_coerce_int(payload.get("port"), 0),
            open=bool(payload.get("open", False)),
            error=_coerce_str(payload.get("error")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "host": self.host,
            "port": self.port,
            "open": bool(self.open),
            "error": self.error,
        }


@dataclass(frozen=True)
class LadderProxyPortsStatusDTO:
    ok: bool = False
    hosts: list[str] = field(default_factory=list)
    ports: list[int] = field(default_factory=list)
    checks: list[LadderProxyPortCheckDTO] = field(default_factory=list)

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyPortsStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        return cls(
            ok=bool(payload.get("ok", False)),
            hosts=[_coerce_str(item) for item in payload.get("hosts", []) if _coerce_str(item)],
            ports=_normalize_ports(payload.get("ports")),
            checks=[
                LadderProxyPortCheckDTO.from_mapping(item)
                for item in payload.get("checks", [])
            ],
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ok": bool(self.ok),
            "hosts": list(self.hosts),
            "ports": list(self.ports),
            "checks": [item.to_dict() for item in self.checks],
        }


@dataclass(frozen=True)
class LadderProxyStatusDTO:
    running: bool = False
    pid: Optional[int] = None
    returncode: Optional[int] = None
    uptime_sec: float = 0.0
    last_error: str = ""
    stdout_tail: list[str] = field(default_factory=list)
    stderr_tail: list[str] = field(default_factory=list)
    validation: Dict[str, Any] = field(default_factory=dict)
    ports: Optional[LadderProxyPortsStatusDTO] = None
    launch_diagnostics: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        ports = payload.get("ports")
        return cls(
            running=bool(payload.get("running", False)),
            pid=_coerce_optional_int(payload.get("pid")),
            returncode=_coerce_optional_int(payload.get("returncode")),
            uptime_sec=_coerce_float(payload.get("uptime_sec"), 0.0),
            last_error=_coerce_str(payload.get("last_error")),
            stdout_tail=[str(item) for item in payload.get("stdout_tail", [])],
            stderr_tail=[str(item) for item in payload.get("stderr_tail", [])],
            validation=_coerce_dict(payload.get("validation")),
            ports=(
                LadderProxyPortsStatusDTO.from_mapping(ports)
                if isinstance(ports, (dict, LadderProxyPortsStatusDTO))
                else None
            ),
            launch_diagnostics=_coerce_dict(payload.get("launch_diagnostics")),
        )

    def to_dict(self) -> Dict[str, Any]:
        payload = {
            "running": bool(self.running),
            "pid": self.pid,
            "returncode": self.returncode,
            "uptime_sec": self.uptime_sec,
            "last_error": self.last_error,
            "stdout_tail": list(self.stdout_tail),
            "stderr_tail": list(self.stderr_tail),
            "launch_diagnostics": dict(self.launch_diagnostics),
        }
        if self.validation:
            payload["validation"] = dict(self.validation)
        if self.ports is not None:
            payload["ports"] = self.ports.to_dict()
        return payload


@dataclass(frozen=True)
class LadderProxyResultDTO:
    ok: bool
    running: bool = False
    status: LadderProxyStatusDTO = field(default_factory=LadderProxyStatusDTO)
    error: Optional[str] = None
    stopped: bool = False
    details: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyResultDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        known_keys = {"ok", "running", "status", "error", "stopped", "details"}
        details = _coerce_dict(payload.get("details"))
        details.update({key: item for key, item in payload.items() if key not in known_keys})
        status = LadderProxyStatusDTO.from_mapping(payload.get("status"))
        running = bool(payload.get("running", status.running))
        return cls(
            ok=bool(payload.get("ok", False)),
            running=running,
            status=status,
            error=None if payload.get("error") is None else str(payload.get("error")),
            stopped=bool(payload.get("stopped", False)),
            details=details,
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ok": bool(self.ok),
            "running": bool(self.running),
            "status": self.status.to_dict(),
            "error": self.error,
            "stopped": bool(self.stopped),
            **dict(self.details),
        }


@dataclass(frozen=True)
class LadderProxyExitEventDTO:
    pid: Optional[int] = None
    returncode: Optional[int] = None
    launch_diagnostics: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyExitEventDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        return cls(
            pid=_coerce_optional_int(payload.get("pid")),
            returncode=_coerce_optional_int(payload.get("returncode")),
            launch_diagnostics=_coerce_dict(payload.get("launch_diagnostics")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "pid": self.pid,
            "returncode": self.returncode,
            "launch_diagnostics": dict(self.launch_diagnostics),
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
    game_result: Optional[GameResultDTO] = None
    game_status: Optional[GameStatusDTO] = None

    def to_dict(self) -> Dict[str, Any]:
        payload = {
            "mode": str(self.mode),
            "result": (
                self.result.to_dict() if isinstance(self.result, StartResultDTO) else _coerce_dict(self.result)
            ),
            "ladder_proxy": _coerce_dict(self.ladder_proxy),
        }
        if isinstance(self.game_result, GameResultDTO):
            payload["game_result"] = self.game_result.to_dict()
        if isinstance(self.game_status, GameStatusDTO):
            payload["game_status"] = self.game_status.to_dict()
        return payload


@dataclass(frozen=True)
class StarCraft2Event:
    event_type: str
    details: Dict[str, Any] = field(default_factory=dict)
    source: str = "starcraft2"
    engine: str = "starcraft2"
    time: float = field(default_factory=lambda: round(time.time(), 6))

    @classmethod
    def from_mapping(cls, value: Any) -> "StarCraft2Event":
        if isinstance(value, cls):
            return value
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
